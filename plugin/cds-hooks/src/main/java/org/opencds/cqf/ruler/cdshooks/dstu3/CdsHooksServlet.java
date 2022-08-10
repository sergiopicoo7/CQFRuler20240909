package org.opencds.cqf.ruler.cdshooks.dstu3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.parser.LenientErrorHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.exception.DataProviderException;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.cdshooks.CdsServicesCache;
import org.opencds.cqf.ruler.cdshooks.providers.ProviderConfiguration;
import org.opencds.cqf.ruler.cdshooks.request.CdsHooksRequest;
import org.opencds.cqf.ruler.cdshooks.response.Cards;
import org.opencds.cqf.ruler.cdshooks.response.ErrorHandling;
import org.opencds.cqf.ruler.cpg.dstu3.provider.LibraryEvaluationProvider;
import org.opencds.cqf.ruler.cql.CqlProperties;
import org.opencds.cqf.ruler.cql.JpaTerminologyProviderFactory;
import org.opencds.cqf.ruler.cr.dstu3.provider.ActivityDefinitionApplyProvider;
import org.opencds.cqf.ruler.external.AppProperties;
import org.opencds.cqf.ruler.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

public class CdsHooksServlet extends HttpServlet implements DaoRegistryUser {

	private static final Logger logger = LoggerFactory
			.getLogger(org.opencds.cqf.ruler.cdshooks.dstu3.CdsHooksServlet.class);

	private static final long serialVersionUID = 1L;

	@Autowired
	private CqlProperties cqlProperties;

	@Autowired
	private DaoRegistry daoRegistry;

	@Autowired
	private AppProperties myAppProperties;

	@Autowired
	private LibraryEvaluationProvider libraryEvaluator;

	@Autowired
	private ActivityDefinitionApplyProvider applyEvaluator;

	@Autowired
	private ProviderConfiguration providerConfiguration;

	@Autowired
	JpaTerminologyProviderFactory myJpaTerminologyProviderFactory;

	@Autowired
	CdsServicesCache cdsServicesCache;

	protected ProviderConfiguration getProviderConfiguration() {
		return this.providerConfiguration;
	}

	private SystemRequestDetails requestDetails;

	// CORS Pre-flight
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ErrorHandling.setAccessControlHeaders(resp, myAppProperties);
		resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		resp.setHeader("X-Content-Type-Options", "nosniff");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info(request.getRequestURI());
		if (!request.getRequestURL().toString().endsWith("/cds-services")
				&& !request.getRequestURL().toString().endsWith("/cds-services/")) {
			logger.error(request.getRequestURI());
			throw new ServletException("This servlet is not configured to handle GET requests.");
		}
		ErrorHandling.setAccessControlHeaders(response, myAppProperties);
		response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
				throw new ServletException(String.format("Invalid content type %s. Please use application/json.",
						request.getContentType()));
			}
			logger.info(request.getRequestURI());
			String baseUrl = myAppProperties.getServer_address();
			String service = request.getPathInfo().replace("/", "");
			ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

			String requestJson = request.getReader().lines().collect(Collectors.joining());
			CdsHooksRequest cdsHooksRequest = mapper.readValue(requestJson, CdsHooksRequest.class);
			logRequestInfo(cdsHooksRequest, requestJson);

			PlanDefinition servicePlan = read(Ids.newId(PlanDefinition.class, service));
			IdType logicId = new IdType(servicePlan.getLibrary().get(0).getReference());

			String patientId;
			Parameters parameters = null;
			if (cdsHooksRequest instanceof CdsHooksRequest.OrderSelect) {
				patientId = ((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.patientId;
				parameters = CdsHooksResources.getParameters(
						((CdsHooksRequest.OrderSelect) cdsHooksRequest).context.draftOrders);
			} else if (cdsHooksRequest instanceof CdsHooksRequest.OrderSign) {
				patientId = ((CdsHooksRequest.OrderSign) cdsHooksRequest).context.patientId;
				parameters = CdsHooksResources.getParameters(
						((CdsHooksRequest.OrderSign) cdsHooksRequest).context.draftOrders);
			} else {
				patientId = cdsHooksRequest.context.patientId;
			}
			List<String> expressions = CdsHooksResources.getExpressions(servicePlan);
			BooleanType useServerData = null;
			Endpoint remoteDataEndpoint = null;
			if (cdsHooksRequest.fhirServer != null && !cdsHooksRequest.fhirServer.equals(baseUrl)) {
				useServerData = new BooleanType(false);
				remoteDataEndpoint = new Endpoint().setAddress(cdsHooksRequest.fhirServer);
				if (cdsHooksRequest.fhirAuthorization != null) {
					remoteDataEndpoint.addHeader(cdsHooksRequest.fhirAuthorization.tokenType + ": "
							+ cdsHooksRequest.fhirAuthorization.accessToken);
				}
			}
			Bundle data = CdsHooksResources.getPrefetchResources(cdsHooksRequest);

			requestDetails = new SystemRequestDetails();
			requestDetails.setFhirServerBase(baseUrl);
			Parameters evaluationResult = libraryEvaluator.evaluate(requestDetails, logicId, patientId,
					expressions, parameters, useServerData, data, null, remoteDataEndpoint,
					null, null);

			List<Cards.Card.Link> links = resolvePlanLinks(servicePlan);
			List<Cards.Card> cards = new ArrayList<>();

			if (servicePlan.hasAction()) {
				resolveServicePlan(servicePlan.getAction(), evaluationResult, patientId, cards, links);
			}

			Cards.parser = new ca.uhn.fhir.parser.JsonParser(getFhirContext(), new LenientErrorHandler());
			Cards result = new Cards();
			result.cards = cards;
			// Using GSON pretty print format as Jackson's is ugly
			String jsonResponse = new GsonBuilder().setPrettyPrinting().create().toJson(
					com.google.gson.JsonParser.parseString(mapper.writeValueAsString(result)));
			logger.info(jsonResponse);
			response.getWriter().println(jsonResponse);
		} catch (BaseServerResponseException e) {
			ErrorHandling.handleError(response, "ERROR: Exception connecting to remote server.", e, myAppProperties);
			logger.error(e.toString());
		} catch (DataProviderException e) {
			ErrorHandling.handleError(response,"ERROR: Exception in DataProvider.", e, myAppProperties);
			logger.error(e.toString());
		} catch (CqlException e) {
			ErrorHandling.handleError(response,"ERROR: Exception in CQL Execution.", e, myAppProperties);
			logger.error(e.toString());
		} catch (Exception e) {
			logger.error(e.toString());
			throw new ServletException("ERROR: Exception in cds-hooks processing.", e);
		}
	}

	private void logRequestInfo(CdsHooksRequest request, String jsonRequest) {
		logger.info(jsonRequest);
		logger.info("cds-hooks hook instance: {}", request.hookInstance);
		logger.info("cds-hooks maxCodesPerQuery: {}", this.getProviderConfiguration().getMaxCodesPerQuery());
		logger.info("cds-hooks expandValueSets: {}", this.getProviderConfiguration().getExpandValueSets());
		logger.info("cds-hooks searchStyle: {}", this.getProviderConfiguration().getSearchStyle());
		logger.info("cds-hooks prefetch maxUriLength: {}", this.getProviderConfiguration().getMaxUriLength());
		logger.info("cds-hooks local server address: {}", myAppProperties.getServer_address());
		logger.info("cds-hooks fhir server address: {}", request.fhirServer);
		logger.info("cds-hooks cql_logging_enabled: {}", this.getProviderConfiguration().getCqlLoggingEnabled());
	}

	private List<Cards.Card.Link> resolvePlanLinks(PlanDefinition servicePlan) {
		List<Cards.Card.Link> links = new ArrayList<>();
		// links - listed on each card
		if (servicePlan.hasRelatedArtifact()) {
			servicePlan.getRelatedArtifact().forEach(
					x -> {
						Cards.Card.Link link = new Cards.Card.Link();
						if (x.hasDisplay()) link.label = x.getDisplay();
						if (x.hasUrl()) link.url = x.getUrl();
						if (x.hasExtension()) link.type = x.getExtensionFirstRep().getValue().primitiveValue();
						else link.type = "absolute"; // default
						links.add(link);
					}
			);
		}
		return links;
	}

	// Assumption that expressions are using CQL and will be references (text/cql.identifier introduced in FHIR R4)
	private void resolveServicePlan(List<PlanDefinition.PlanDefinitionActionComponent> actions,
									Parameters evaluationResults, String patientId, List<Cards.Card> cards,
									List<Cards.Card.Link> links) {
		Cards.Card card = new Cards.Card();
		if (links != null) card.links = links;
		actions.forEach(
				x -> {
					AtomicBoolean conditionMet = new AtomicBoolean(false);
					if (x.hasCondition()) {
						x.getCondition().forEach(
								xx -> {
									if (xx.hasExpression()) {
										Type conditionResult = evaluationResults.getParameter().stream().filter(
													p -> p.getName().equals(xx.getExpression())).findFirst().get().getValue();
										conditionMet.set(conditionResult.isPrimitive()
												&& Boolean.parseBoolean(conditionResult.primitiveValue()));
									}
								}
						);
					}
					if (conditionMet.get()) {
						if (x.hasTitle()) card.summary = x.getTitle();
						if (x.hasDescription()) card.detail = x.getDescription();
						if (x.hasDocumentation()) {
							Cards.Card.Source source = new Cards.Card.Source();
							RelatedArtifact documentation = x.getDocumentationFirstRep();
							if (documentation.hasDisplay()) source.label = documentation.getDisplay();
							if (documentation.hasUrl()) source.uri = documentation.getUrl();
							if (documentation.hasDocument() && documentation.getDocument().hasUrl()) {
								source.icon = documentation.getDocument().getUrl();
							}
							card.source = source;
						}
						if (x.hasSelectionBehavior()) card.selectionBehavior = x.getSelectionBehavior().toCode();
						if (x.hasLabel()) {
							Cards.Card.Suggestion suggestion = new Cards.Card.Suggestion();
							Cards.Card.Suggestion.Action suggAction = new Cards.Card.Suggestion.Action();
							suggestion.label = x.getLabel();
							boolean hasAction = false;
							if (x.hasDescription()) {
								suggAction.description = x.getDescription();
								hasAction = true;
							}
							if (x.hasType() && x.getType().hasCode()
									&& !x.getType().getCode().equals("fire-event")) {
								String actionCode = x.getType().getCode();
								suggAction.type = actionCode.equals("remove") ? "delete" : actionCode;
								hasAction = true;
							}
							if (x.hasDefinition() && x.getDefinition().hasReference()
									&& x.getDefinition().getReference().contains("ActivityDefinition")) {
								suggAction.type = "create";
								IdType definitionId = new IdType(x.getDefinition().getReference());
								suggAction.resource = applyEvaluator.apply(requestDetails, definitionId,
										patientId, null, null, null, null,
										null, null, null, null);
								hasAction = true;
							}
							if (hasAction) suggestion.actions = Collections.singletonList(suggAction);
							card.suggestions = Collections.singletonList(suggestion);
						}
						if (x.hasDynamicValue()) {
							x.getDynamicValue().forEach(
									xx -> {
										if (xx.hasPath() && xx.hasExpression()) {
											Object dynamicValueResult = evaluationResults.getParameter().stream().filter(
													p -> p.getName().equals(xx.getExpression())).findFirst().get().getValue();
											if (xx.getPath().endsWith("title")) {
												card.summary = dynamicValueResult.toString();
											}
											else if (xx.getPath().endsWith("description")) {
												card.detail = dynamicValueResult.toString();
												if (card.suggestions != null
														&& card.suggestions.get(0).actions != null) {
													card.suggestions.get(0).actions.get(0).description =
															dynamicValueResult.toString();
												}
											}
											else if (xx.getPath().endsWith("extension")) {
												card.indicator = dynamicValueResult.toString();
											}
											else if (card.suggestions != null
													&& card.suggestions.get(0).actions != null
													&& card.suggestions.get((0)).actions.get(0).resource != null) {
												new Dstu3FhirModelResolver().setValue(
														card.suggestions.get((0)).actions.get(0).resource,
														xx.getPath(), dynamicValueResult);
											}
										}
									}
							);
						}
						if (x.hasAction()) {
							resolveServicePlan(x.getAction(), evaluationResults, patientId, cards, links);
						}
						cards.add(card);
					}
				}
		);
	}

	private JsonObject getServices() {
		JsonObject services = new JsonObject();
		services.add("services", this.cdsServicesCache.getCdsServiceCache().get());
		return services;
	}

	public DebugMap getDebugMap() {
		DebugMap debugMap = new DebugMap();
		if (cqlProperties.getOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
			debugMap.setIsLoggingEnabled(true);
		}
		return debugMap;
	}

	@Override
	public DaoRegistry getDaoRegistry() {
		return this.daoRegistry;
	}
}
