package org.opencds.cqf.ruler.casereporting.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactApproveVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactDraftVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactPackageVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactReleaseVisitor;
import org.opencds.cqf.ruler.casereporting.IBaseSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CaseReportingOperationProvider {
	@Autowired
	private IRepositoryFactory repositoryFactory;

	@Autowired
	private DaoRegistry daoRegistry;

	@Autowired
	private FhirContext fhirContext;

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	private AdapterFactory adapterFactory = AdapterFactory.forFhirVersion(FhirVersionEnum.R4);

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails                    the {@link RequestDetails RequestDetails}
	 * @param theId                             the {@link IdType IdType}, always an argument for instance level operations
	 * @param approvalDate                      Optional Date parameter for indicating the date of approval
	 *                                          for an approval submission. If approvalDate is not
	 *                                          provided, the current date will be used.
	 * @param artifactAssessmentType
	 * @param artifactAssessmentSummary
	 * @param artifactAssessmentTarget
	 * @param artifactAssessmentRelatedArtifact
	 * @param artifactAssessmentAuthor          Optional ArtifactAssessment* arguments represent parts of a
	 *                                          comment to beincluded as part of the approval. The
	 *                                          artifactAssessment is a crmi-artifactAssessment as defined here:
	 *                                          http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessment
	 *                                          A Parameters resource with a parameter for each element
	 *                                          of the ArtifactAssessment Extension definition is
	 *                                          used to represent the proper structure.
	 * @return An IBaseResource that is the targeted resource, updated with the approval
	 */
	@Operation(name = "$approve", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$approve", value = "Apply an approval to an existing artifact, regardless of status.")
	public Bundle approveOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "approvalDate", typeName = "Date") IPrimitiveType<Date> approvalDate,
		@OperationParam(name = "artifactAssessmentType") String artifactAssessmentType,
		@OperationParam(name = "artifactAssessmentSummary") String artifactAssessmentSummary,
		@OperationParam(name = "artifactAssessmentTarget") CanonicalType artifactAssessmentTarget,
		@OperationParam(name = "artifactAssessmentRelatedArtifact") CanonicalType artifactAssessmentRelatedArtifact,
		@OperationParam(name = "artifactAssessmentAuthor") Reference artifactAssessmentAuthor) throws UnprocessableEntityException {
		var repository = repositoryFactory.create(requestDetails);
		var resource = (MetadataResource) SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		if (artifactAssessmentTarget != null) {
			if (Canonicals.getUrl(artifactAssessmentTarget) != null
				&& !Canonicals.getUrl(artifactAssessmentTarget).equals(resource.getUrl())) {
				throw new UnprocessableEntityException("ArtifactAssessmentTarget URL does not match URL of resource being approved.");
			}
			if (Canonicals.getVersion(artifactAssessmentTarget) != null
				&& !Canonicals.getVersion(artifactAssessmentTarget).equals(resource.getVersion())) {
				throw new UnprocessableEntityException("ArtifactAssessmentTarget version does not match version of resource being approved.");
			}
		} else if (artifactAssessmentTarget == null) {
			String target = "";
			String url = resource.getUrl();
			String version = resource.getVersion();
			if (url != null) {
				target += url;
			}
			if (version != null) {
				if (url != null) {
					target += "|";
				}
				target += version;
			}
			if (target != null) {
				artifactAssessmentTarget = new CanonicalType(target);
			}
		}
		var params = new Parameters();
		if (approvalDate != null && approvalDate.hasValue()) {
			params.addParameter("approvalDate", new DateType(approvalDate.getValue()));
		}
		if (artifactAssessmentType != null) {
			params.addParameter("artifactAssessmentType", artifactAssessmentType);
		}
		if (artifactAssessmentTarget != null) {
			params.addParameter("artifactAssessmentTarget", artifactAssessmentTarget);
		}
		if (artifactAssessmentSummary != null) {
			params.addParameter("artifactAssessmentSummary", artifactAssessmentSummary);
		}
		if (artifactAssessmentRelatedArtifact != null) {
			params.addParameter("artifactAssessmentRelatedArtifact", artifactAssessmentRelatedArtifact);
		}
		if (artifactAssessmentAuthor != null) {
			params.addParameter("artifactAssessmentAuthor", artifactAssessmentAuthor);
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactApproveVisitor();
		return ((Bundle) adapter.accept(visitor, repository, params));
	}

	/**
	 * Creates a draft of an existing artifact if it has status Active.
	 *
	 * @param requestDetails the {@link RequestDetails RequestDetails}
	 * @param theId          the {@link IdType IdType}, always an argument for instance level operations
	 * @param version        new version in the form MAJOR.MINOR.PATCH
	 * @return A transaction bundle result of the newly created resources
	 */
	@Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	public Bundle draftOperation(RequestDetails requestDetails, @IdParam IdType theId, @OperationParam(name = "version") String version)
		throws FHIRException {
		var repository = repositoryFactory.create(requestDetails);
		var resource = (MetadataResource) SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters().addParameter("version", new StringType(version));
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactDraftVisitor();
		return ((Bundle) adapter.accept(visitor, repository, params));
	}

	/**
	 * Sets the status of an existing artifact to Active if it has status Draft.
	 *
	 * @param requestDetails     the {@link RequestDetails RequestDetails}
	 * @param theId              the {@link IdType IdType}, always an argument for instance level operations
	 * @param version            new version in the form MAJOR.MINOR.PATCH
	 * @param versionBehavior    how to handle differences between the user-provided and incumbernt versions
	 * @param latestFromTxServer whether or not to query the TxServer if version information is missing from references
	 * @return A transaction bundle result of the updated resources
	 */
	@Operation(name = "$release", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$release", value = "Release an existing draft artifact")
	public Bundle releaseOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		@OperationParam(name = "version") String version,
		@OperationParam(name = "versionBehavior") CodeType versionBehavior,
		@OperationParam(name = "latestFromTxServer", typeName = "Boolean") IPrimitiveType<Boolean> latestFromTxServer,
		@OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
		@OperationParam(name = "releaseLabel") String releaseLabel)
		throws FHIRException {
		var repository = repositoryFactory.create(requestDetails);
		var resource = (MetadataResource) SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters();
		if (version != null) {
			params.addParameter("version", version);
		}
		if (versionBehavior != null) {
			params.addParameter("versionBehavior", versionBehavior);
		}
		if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
			params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
		}
		if (requireNonExperimental != null) {
			params.addParameter("requireNonExperimental", requireNonExperimental);
		}
		if (releaseLabel != null) {
			params.addParameter("releaseLabel", releaseLabel);
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		try {
			var visitor = new KnowledgeArtifactReleaseVisitor();
			var retval = (Bundle) adapter.accept(visitor, repository, params);
			// not copying the extensions correctly and not releasing all the
			// artifacts correctly (still have draft references)
			forEachMetadataResource(
				retval.getEntry(),
				(r) -> {
					if (r != null) {
						adapterFactory.createKnowledgeArtifactAdapter(r).getRelatedArtifact()
							.forEach(ra -> {
								KnowledgeArtifactProcessor.checkIfValueSetNeedsCondition(null, (RelatedArtifact) ra, repository);
							});
					}
				},
				repository);
			return retval;
		} catch (Exception e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
	}

	private void forEachMetadataResource(List<Bundle.BundleEntryComponent> entries, Consumer<MetadataResource> callback, Repository repository) {
		entries.stream()
			.map(entry -> entry.getResponse().getLocation())
			.map(location -> {
				switch (location.split("/")[0]) {
					case "ActivityDefinition":
						return repository.read(ActivityDefinition.class, new IdType(location));
					case "Library":
						return repository.read(Library.class, new IdType(location));
					case "Measure":
						return repository.read(Measure.class, new IdType(location));
					case "PlanDefinition":
						return repository.read(PlanDefinition.class, new IdType(location));
					case "ValueSet":
						return repository.read(ValueSet.class, new IdType(location));
					default:
						return null;
				}
			})
			.forEach(callback);
	}

	@Operation(name = "$package", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$package", value = "Package an artifact and components / dependencies")
	public Bundle packageOperation(
		RequestDetails requestDetails,
		@IdParam IdType theId,
		// TODO: $package - should capability be CodeType?
		@OperationParam(name = "capability") List<String> capability,
		@OperationParam(name = "artifactVersion") List<CanonicalType> artifactVersion,
		@OperationParam(name = "checkArtifactVersion") List<CanonicalType> checkArtifactVersion,
		@OperationParam(name = "forceArtifactVersion") List<CanonicalType> forceArtifactVersion,
		// TODO: $package - should include be CodeType?
		@OperationParam(name = "include") List<String> include,
		@OperationParam(name = "manifest") CanonicalType manifest,
		@OperationParam(name = "offset", typeName = "Integer") IPrimitiveType<Integer> offset,
		@OperationParam(name = "count", typeName = "Integer") IPrimitiveType<Integer> count,
		@OperationParam(name = "packageOnly", typeName = "Boolean") IPrimitiveType<Boolean> packageOnly,
		@OperationParam(name = "artifactEndpointConfiguration") Parameters.ParametersParameterComponent artifactEndpointConfiguration,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint
	) throws FHIRException {
		var repository = repositoryFactory.create(requestDetails);
		var resource = (MetadataResource) SearchHelper.readRepository(repository, theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = new Parameters();
		if (manifest != null) {
			params.addParameter("manifest", manifest);
		}
		if (artifactEndpointConfiguration != null) {
			params.addParameter().setName("artifactEndpointConfiguration").addPart(artifactEndpointConfiguration);
		}
		if (offset != null && offset.hasValue()) {
			params.addParameter("offset", new IntegerType(offset.getValue()));
		}
		if (offset != null && offset.hasValue()) {
			params.addParameter("offset", new IntegerType(offset.getValue()));
		}
		if (count != null && count.hasValue()) {
			params.addParameter("count", new IntegerType(count.getValue()));
		}
		if (packageOnly != null && packageOnly.hasValue()) {
			params.addParameter("packageOnly", packageOnly.getValue());
		}
		if (capability != null) {
			capability.forEach(c -> params.addParameter("capability", c));
		}
		if (artifactVersion != null) {
			artifactVersion.forEach(a -> params.addParameter("artifactVersion", a));
		}
		if (checkArtifactVersion != null) {
			checkArtifactVersion.forEach(c -> params.addParameter("checkArtifactVersion", c));
		}
		if (forceArtifactVersion != null) {
			forceArtifactVersion.forEach(f -> params.addParameter("forceArtifactVersion", f));
		}
		if (include != null) {
			include.forEach(i -> params.addParameter("include", i));
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactPackageVisitor();
		var retval = (Bundle) adapter.accept(visitor, repository, params);
		retval.getEntry().stream()
			.map(e -> (MetadataResource) e.getResource())
			.filter(r -> {
				var id1 = r.getResourceType().toString() + "/" + r.getIdPart();
				var id2 = theId.getValue();
				return id1.equals(id2);
			})
			.findFirst()
			.ifPresent(m -> {
				KnowledgeArtifactProcessor.handleValueSetReferenceExtensions(m, retval.getEntry(), repository);
			});
		return retval;
	}

	@Operation(name = "$revise", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
	public IBaseResource reviseOperation(RequestDetails requestDetails, @OperationParam(name = "resource") IBaseResource resource)
		throws FHIRException {
		var repository = repositoryFactory.create(requestDetails);
		return (IBaseResource) this.artifactProcessor.revise(repository, (MetadataResource) resource);
	}

	@Operation(name = "$validate", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$validate", value = "Validate a bundle")
	public OperationOutcome validateOperation(RequestDetails requestDetails,
															@OperationParam(name = "resource") IBaseResource resource,
															@OperationParam(name = "mode") CodeType mode,
															@OperationParam(name = "profile") String profile
	)
		throws FHIRException {
		if (mode != null) {
			throw new NotImplementedOperationException("'mode' Parameter not implemented yet.");
		}
		if (profile != null) {
			throw new NotImplementedOperationException("'profile' Parameter not implemented yet.");
		}
		if (resource == null) {
			throw new UnprocessableEntityException("A FHIR resource must be provided for validation");
		}
		var ctx = fhirContext;
		if (ctx != null) {
			var fhirValidator = ctx.newValidator();
			fhirValidator.setValidateAgainstStandardSchema(false);
			fhirValidator.setValidateAgainstStandardSchematron(false);
			var npm = new NpmPackageValidationSupport(ctx);
			try {
				npm.loadPackageFromClasspath("classpath:hl7.fhir.us.ecr-2.1.0.tgz");
			} catch (IOException e) {
				throw new InternalErrorException("Could not load package");
			}
			var chain = new ValidationSupportChain(
				npm,
				new DefaultProfileValidationSupport(ctx),
				new InMemoryTerminologyServerValidationSupport(ctx),
				new CommonCodeSystemsTerminologyService(ctx)
			);
			var instanceValidatorModule = new FhirInstanceValidator(chain);
			instanceValidatorModule.setValidatorResourceFetcher(new ValidatorResourceFetcher(ctx, chain, daoRegistry));
			fhirValidator.registerValidatorModule(instanceValidatorModule);
			return (OperationOutcome) fhirValidator.validateWithResult(resource, null).toOperationOutcome();
		} else {
			throw new InternalErrorException("Could not load FHIR Context");
		}
	}

	@Operation(name = "$artifact-diff", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$artifact-diff", value = "Diff two knowledge artifacts")
	public Parameters crmiArtifactDiff(RequestDetails requestDetails,
												  @OperationParam(name = "source") String source,
												  @OperationParam(name = "target") String target,
												  @OperationParam(name = "compareExecutable", typeName = "Boolean") IPrimitiveType<Boolean> compareExecutable,
												  @OperationParam(name = "compareComputable", typeName = "Boolean") IPrimitiveType<Boolean> compareComputable
	) throws UnprocessableEntityException, ResourceNotFoundException {
		var repository = repositoryFactory.create(requestDetails);
		var sourceId = new IdType(source);
		var theSourceResource = SearchHelper.readRepository(repository, sourceId);
		if (theSourceResource == null || !(theSourceResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Source resource must exist and be a Knowledge Artifact type.");
		}
		var targetId = new IdType(target);
		var theTargetResource = SearchHelper.readRepository(repository, targetId);
		if (theTargetResource == null || !(theTargetResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Target resource must exist and be a Knowledge Artifact type.");
		}
		if (theSourceResource.getClass() != theTargetResource.getClass()) {
			throw new UnprocessableEntityException("Source and target resources must be of the same type.");
		}
		var dao = (IFhirResourceDaoValueSet<ValueSet>) daoRegistry.getResourceDao(ValueSet.class);
		return this.artifactProcessor.artifactDiff((MetadataResource) theSourceResource, (MetadataResource) theTargetResource, fhirContext, repository, compareComputable == null ? false : compareComputable.getValue(), compareExecutable == null ? false : compareExecutable.getValue(), dao, null);
	}

	@Operation(name = "$create-changelog", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$create-changelog", value = "Create a changelog object which can be easily rendered into a table")
	public IBaseResource flattenDiffParametersToChangeLogJSON(RequestDetails requestDetails,
																				 @OperationParam(name = "source") String source,
																				 @OperationParam(name = "target") String target) {
		// 1) Create Diff Parameters Object as input
		var cache = new KnowledgeArtifactProcessor.diffCache();
		var repository = repositoryFactory.create(requestDetails);
		var dao = (IFhirResourceDaoValueSet<ValueSet>) daoRegistry.getResourceDao(ValueSet.class);
		var sourceId = new IdType(source);
		var theSourceResource = (MetadataResource) SearchHelper.readRepository(repository, sourceId);
		if (theSourceResource == null || !(theSourceResource instanceof Library)) {
			throw new UnprocessableEntityException("Source resource must exist and be a Library.");
		}
		var targetId = new IdType(target);
		var theTargetResource = (MetadataResource) SearchHelper.readRepository(repository, targetId);
		if (theTargetResource == null || !(theTargetResource instanceof Library)) {
			throw new UnprocessableEntityException("Target resource must exist and be a Libary.");
		}
		var targetAdapter = AdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(theTargetResource);
		var diffParameters = this.artifactProcessor.artifactDiff(theSourceResource, theTargetResource, fhirContext, repository, true, true, dao, cache);
		var manifestUrl = targetAdapter.getUrl();
		var changelog = new ChangeLog(manifestUrl);

		// 2) Recursively process the Parameters into a flat ChangeLog
		processChanges(diffParameters.getParameter(), changelog, cache, manifestUrl);

		// 3) Handle the Conditions and Priorities which are in RelatedArtifact changes
		changelog.handleRelatedArtifacts();

		// 4) Generate the output JSON
		var bin = new Binary();
		var mapper = createSerializer();
		try {
			bin.setContent(mapper.writeValueAsString(changelog).getBytes(Charset.forName("UTF-8")));
		} catch (JsonProcessingException e) {
			// TODO: handle exception
			throw new UnprocessableEntityException(e.getMessage());
		}

		return bin;
	}

	private ObjectMapper createSerializer() {
		var mapper = new ObjectMapper()
			.setSerializationInclusion(JsonInclude.Include.NON_NULL)
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		SimpleModule module = new SimpleModule("IBaseSerializer", new Version(1, 0, 0, null, null, null));
		module.addSerializer(IBase.class, new IBaseSerializer(fhirContext));
		mapper.registerModule(module);
		return mapper;
	}

	private void processChanges(List<Parameters.ParametersParameterComponent> changes, ChangeLog changelog, KnowledgeArtifactProcessor.diffCache cache, String url) {
		// 1) Get the source and target resources so we can pull additional info as necessary
		var resources = cache.getResourcesForUrl(url);
		var resourceType = Canonicals.getResourceType(url);
		// Check if the resource pair was already processed
		var wasPageAlreadyProcessed = changelog.getPage(url).isPresent();
		if (!resources.isEmpty() && !wasPageAlreadyProcessed) {
			final MetadataResource sourceResource = resources.get(0).isSource ? resources.get(0).resource : (resources.size() > 1 ? resources.get(1).resource : null);
			final MetadataResource targetResource = resources.get(0).isSource ? (resources.size() > 1 ? resources.get(1).resource : null) : resources.get(0).resource;

			// 2) Generate a page for each resource pair based on ResourceType
			var page = changelog.getPage(url).orElseGet(() -> {
				switch (resourceType) {
					case "ValueSet":
						return changelog.addPage((ValueSet) sourceResource, (ValueSet) targetResource, cache);
					case "Library":
						return changelog.addPage((Library) sourceResource, (Library) targetResource);
					case "PlanDefinition":
						return changelog.addPage((PlanDefinition) sourceResource, (PlanDefinition) targetResource);
					default:
						throw new UnprocessableEntityException("Unknown resource type: " + resourceType);
				}
			});
			for (var change : changes) {
				if (change.hasName() && !change.getName().equals("operation")
					&& change.hasResource() && change.getResource() instanceof Parameters) {
					// Nested Parameters objects get recursively processed
					processChanges(((Parameters) change.getResource()).getParameter(), changelog, cache, change.getName());
				} else if (change.getName().equals("operation")) {
					// 3) For each operation get the relevant parameters
					var type = getStringParameter(change, "type")
						.orElseThrow(() -> new UnprocessableEntityException("Type must be provided when adding an operation to the ChangeLog"));
					var newValue = getParameter(change, "value");
					var path = getPathParameterNoBase(change);
					var originalValue = getParameter(change, "previousValue").map(o -> (Object) o);
					// try to extract the original value from the
					// source object if not present in the Diff
					// Parameters object
					try {
						if (originalValue.isEmpty()) {
							originalValue = Optional.ofNullable((new PropertyUtilsBean()).getProperty(sourceResource, path.get()));
						}
					} catch (Exception e) {
						// TODO: handle exception
						// var message = e.getMessage();
						throw new InternalErrorException("Could not process path: " + path + ": " + e.getMessage());
					}

					// 4) Add a new operation to the ChangeLog
					page.addOperation(type, path.orElse(null), newValue.orElse(null), originalValue.orElse(null), changelog);
				} else {
					// 5) Ignore the changelog entries for deleted or not owned entries
					var thing = change;
					var name = change.getName();
					var hasResource = change.hasResource();
					var getResource = change.getResource();
					var instanceofparam = change.getResource() instanceof Parameters;
				}
			}
		}
	}

	private Optional<String> getPathParameterNoBase(Parameters.ParametersParameterComponent change) {
		return getStringParameter(change, "path").map(p -> {
			var e = new EncodeContextPath(p);
			var removeBase = removeBase(e);
			return removeBase;
		});
	}

	private String removeBase(EncodeContextPath path) {
		return path.getPath().subList(1, path.getPath().size())
			.stream()
			.map(t -> t.toString())
			.collect(Collectors.joining("."));
	}

	private Optional<String> getStringParameter(Parameters.ParametersParameterComponent part, String name) {
		return part.getPart().stream()
			.filter(p -> p.getName().equalsIgnoreCase(name))
			.filter(p -> p.getValue() instanceof IPrimitiveType)
			.map(p -> (IPrimitiveType) p.getValue())
			.map(s -> (String) s.getValue())
			.findAny();
	}

	private Optional<IBase> getParameter(Parameters.ParametersParameterComponent part, String name) {
		return part.getPart().stream()
			.filter(p -> p.getName().equalsIgnoreCase(name))
			.filter(p -> p.hasValue())
			.map(p -> (IBase) p.getValue())
			.findAny();
	}
}
