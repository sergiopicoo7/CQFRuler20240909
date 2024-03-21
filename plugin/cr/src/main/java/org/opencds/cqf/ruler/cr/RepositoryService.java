package org.opencds.cqf.ruler.cr;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
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
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactApproveVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactDraftVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactPackageVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactReleaseVisitor;
import org.opencds.cqf.ruler.cr.r4.helper.ResourceClassMapHelper;
import org.opencds.cqf.ruler.provider.HapiFhirRepositoryProvider;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;

public class RepositoryService extends HapiFhirRepositoryProvider {

	@Autowired
	private KnowledgeArtifactProcessor artifactProcessor;

	private AdapterFactory adapterFactory = AdapterFactory.forFhirVersion(FhirVersionEnum.R4);

	/**
	 * Applies an approval to an existing artifact, regardless of status.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					the {@link IdType IdType}, always an argument for instance level operations
	 * @param approvalDate        Optional Date parameter for indicating the date of approval
	 *                            for an approval submission. If approvalDate is not
	 *                           	provided, the current date will be used.
	 * @param artifactAssessmentType
	 * @param artifactAssessmentSummary
	 * @param artifactAssessmentTarget
	 * @param artifactAssessmentRelatedArtifact
	 * @param artifactAssessmentAuthor Optional ArtifactComment* arguments represent parts of a
	 *                            comment to beincluded as part of the approval. The
	 *                            artifactComment is a cqfm-artifactComment as defined here:
	 *                            http://hl7.org/fhir/us/cqfmeasures/STU3/StructureDefinition-cqfm-artifactComment.html
	 *                            A Parameters resource with a parameter for each element
	 *                            of the artifactComment Extension definition is
	 *                            used to represent the proper structure.
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
				HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
				MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(theId.getResourceType()), theId);
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
		var params = parameters();
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
		return((Bundle)adapter.accept(visitor, hapiFhirRepository, params));
	}
	/**
	 * Creates a draft of an existing artifact if it has status Active.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					the {@link IdType IdType}, always an argument for instance level operations
	 * @param version             new version in the form MAJOR.MINOR.PATCH
	 * @return A transaction bundle result of the newly created resources
	 */
	@Operation(name = "$draft", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact")
	public Bundle draftOperation(RequestDetails requestDetails, @IdParam IdType theId, @OperationParam(name = "version") String version)
		throws FHIRException {
		HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
		MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(theId.getResourceType()), theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = parameters(
			part("version", new StringType(version))
		);
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		var visitor = new KnowledgeArtifactDraftVisitor();
		return((Bundle)adapter.accept(visitor, hapiFhirRepository, params));
	}
	/**
	 * Sets the status of an existing artifact to Active if it has status Draft.
	 *
	 * @param requestDetails      the {@link RequestDetails RequestDetails}
	 * @param theId					      the {@link IdType IdType}, always an argument for instance level operations
	 * @param version             new version in the form MAJOR.MINOR.PATCH
	 * @param versionBehavior     how to handle differences between the user-provided and incumbernt versions
	 * @param latestFromTxServer  whether or not to query the TxServer if version information is missing from references
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
		HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
		MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(theId.getResourceType()), theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = parameters();
		if (version != null) {
			params.addParameter("version", version);
		}
		if (versionBehavior != null ) {
			params.addParameter("versionBehavior", versionBehavior);
		}
		if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
			params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
		}
		if (requireNonExperimental != null) {
			params.addParameter("requireNonExperimental", requireNonExperimental);
		}
		if (releaseLabel != null ) {
			params.addParameter("releaseLabel", releaseLabel);
		}
		var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
		try {
			var visitor = new KnowledgeArtifactReleaseVisitor();
			var retval = (Bundle)adapter.accept(visitor, hapiFhirRepository, params);
			forEachMetadataResource(
				retval.getEntry(), 
				(r) -> {
					if (r != null) {
						adapterFactory.createKnowledgeArtifactAdapter(r).getRelatedArtifact()
						.forEach(ra -> {
							KnowledgeArtifactProcessor.checkIfValueSetNeedsCondition(null, (RelatedArtifact)ra, hapiFhirRepository);
						});
					}
				}, 
				hapiFhirRepository);
			return retval;
		} catch (Exception e) {
			throw new UnprocessableEntityException(e.getMessage());
		}
	}
	private void forEachMetadataResource(List<BundleEntryComponent> entries, Consumer<MetadataResource> callback, Repository repository) {
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
		@OperationParam(name = "artifactEndpointConfiguration") ParametersParameterComponent artifactEndpointConfiguration,
		@OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint
		) throws FHIRException {
		HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
		MetadataResource resource = (MetadataResource)hapiFhirRepository.read(ResourceClassMapHelper.getClass(theId.getResourceType()), theId);
		if (resource == null) {
			throw new ResourceNotFoundException(theId);
		}
		var params = parameters();
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
		var retval = (Bundle)adapter.accept(visitor, hapiFhirRepository, params);
		retval.getEntry().stream()
			.map(e -> (MetadataResource)e.getResource())
			.filter(r -> {
				var id1 = r.getResourceType().toString() + "/" + r.getIdPart();
				var id2 = theId.getValue();
				return id1.equals(id2);
			})
			.findFirst()
			.ifPresent(m -> {
				KnowledgeArtifactProcessor.handleValueSetReferenceExtensions(m, retval.getEntry(), hapiFhirRepository);
			});
			return retval;
	}

	@Operation(name = "$revise", idempotent = true, global = true, type = MetadataResource.class)
	@Description(shortDefinition = "$revise", value = "Update an existing artifact in 'draft' status")
	public IBaseResource reviseOperation(RequestDetails requestDetails, @OperationParam(name = "resource") IBaseResource resource)
		throws FHIRException {
		HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
		return (IBaseResource)this.artifactProcessor.revise(hapiFhirRepository, (MetadataResource) resource);
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
		FhirContext ctx = this.getFhirContext();
		if (ctx != null) {
			FhirValidator fhirValidator = ctx.newValidator();
			fhirValidator.setValidateAgainstStandardSchema(false);
			fhirValidator.setValidateAgainstStandardSchematron(false);
			NpmPackageValidationSupport npm = new NpmPackageValidationSupport(ctx);
			try {
				npm.loadPackageFromClasspath("classpath:hl7.fhir.us.ecr-2.1.0.tgz");
			} catch (IOException e) {
				throw new InternalErrorException("Could not load package");
			}
			ValidationSupportChain chain = new ValidationSupportChain(
				npm,
				new DefaultProfileValidationSupport(ctx),
				new InMemoryTerminologyServerValidationSupport(ctx),
				new CommonCodeSystemsTerminologyService(ctx)
			);
			FhirInstanceValidator instanceValidatorModule = new FhirInstanceValidator(chain);
			instanceValidatorModule.setValidatorResourceFetcher(new ValidatorResourceFetcher(ctx, chain, getDaoRegistry()));
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
	) throws UnprocessableEntityException, ResourceNotFoundException{
		HapiFhirRepository hapiFhirRepository = this.getRepository(requestDetails);
		IdType sourceId = new IdType(source);
		IBaseResource theSourceResource = hapiFhirRepository.read(ResourceClassMapHelper.getClass(sourceId.getResourceType()), sourceId);
		if (theSourceResource == null || !(theSourceResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Source resource must exist and be a Knowledge Artifact type.");
		}
		IdType targetId = new IdType(target);
		IBaseResource theTargetResource = hapiFhirRepository.read(ResourceClassMapHelper.getClass(targetId.getResourceType()), targetId);
		if (theTargetResource == null || !(theTargetResource instanceof MetadataResource)) {
			throw new UnprocessableEntityException("Target resource must exist and be a Knowledge Artifact type.");
		}
		if (theSourceResource.getClass() != theTargetResource.getClass()) {
			throw new UnprocessableEntityException("Source and target resources must be of the same type.");
		}
		IFhirResourceDaoValueSet<ValueSet> dao = (IFhirResourceDaoValueSet<ValueSet>)this.getDaoRegistry().getResourceDao(ValueSet.class);
		return this.artifactProcessor.artifactDiff((MetadataResource)theSourceResource, (MetadataResource)theTargetResource, this.getFhirContext(), hapiFhirRepository, compareComputable == null ? false : compareComputable.getValue(), compareExecutable == null ? false : compareExecutable.getValue(), dao);
	}
	
}
