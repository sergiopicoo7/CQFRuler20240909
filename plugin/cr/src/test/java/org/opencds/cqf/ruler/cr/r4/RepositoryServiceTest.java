package org.opencds.cqf.ruler.cr.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.cr.KnowledgeArtifactAdapter;
import org.opencds.cqf.ruler.cr.KnowledgeArtifactProcessor;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Lazy
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {RepositoryServiceTest.class, CrConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
class RepositoryServiceTest extends RestIntegrationTest {
	private final String specificationLibReference = "Library/SpecificationLibrary";
	private final String minimalLibReference = "Library/SpecificationLibraryDraftVersion-1-1-1-23";
	private final List<String> badVersionList = Arrays.asList(
			"11asd1",
			"1.1.3.1.1",
			"1.|1.1.1",
			"1/.1.1.1",
			"-1.-1.2.1",
			"1.-1.2.1",
			"1.1.-2.1",
			"7.1..21",
			"1.2.1.3-draft",
			"1.2.3-draft",
			"3.2",
			"1.",
			"3.ad.2.",
			"",
			null
		);
	@Test
	void draftOperation_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		Library baseLib = getClient()
		.read()
		.resource(Library.class)
		.withId(specificationLibReference.split("/")[1])
		.execute();
		// Root Artifact must have approval date, releaseLabel and releaseDescription for this test
		assertTrue(baseLib.hasApprovalDate());
		assertTrue(baseLib.hasExtension(KnowledgeArtifactProcessor.releaseDescriptionUrl));
		assertTrue(baseLib.hasExtension(KnowledgeArtifactProcessor.releaseLabelUrl));
		assertTrue(baseLib.hasApprovalDate());
		String version = "1.0.1.23";
		String draftedVersion = version + "-draft";
		Parameters params = parameters(part("version", version) );
		Bundle returnedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedBundle);
		Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findAny();
		assertTrue(maybeLib.isPresent());
		Library lib = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertNotNull(lib);
		assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
		assertTrue(lib.getVersion().equals(draftedVersion));
		assertFalse(lib.hasApprovalDate());
		assertFalse(lib.hasExtension(KnowledgeArtifactProcessor.releaseDescriptionUrl));
		assertFalse(lib.hasExtension(KnowledgeArtifactProcessor.releaseLabelUrl));
		List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()).equals(draftedVersion));
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()).equals(draftedVersion));
	}
	@Test
	void draftOperation_version_conflict_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = parameters(part("version", "1.1.1.23") );
		String maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("already exists"));
	}
	
	@Test
	void draftOperation_cannot_create_draft_of_draft_test() {
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = parameters(part("version", "1.2.1.23") );
		String maybeException = "";
		try {
			getClient().operation()
			.onInstance(minimalLibReference)
			.named("$crmi.draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("status of 'active'"));
	}
	@Test
	void draftOperation_wrong_id_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Parameters params = parameters(part("version", "1.3.1.23") );
		ResourceNotFoundException maybeException = null;
		try {
			getClient().operation()
			.onInstance("Library/there-is-no-such-id")
			.named("$crmi.draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (ResourceNotFoundException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}
	@Test
	void draftOperation_version_format_test() {
		loadResource("minimal-draft-to-test-version-conflict.json");
		for(String version:badVersionList){
			UnprocessableEntityException maybeException = null;
			Parameters params = parameters(part("version", new StringType(version)) );
			try {
				getClient().operation()
				.onInstance(minimalLibReference)
				.named("$crmi.draft")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}

	@Test
	void releaseResource_test() {
		loadTransaction("ersd-release-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String existingVersion = "1.2.3";
		String versionData = "1.2.7.23";

		Parameters params1 = parameters(
			part("version", new StringType(versionData)),
			part("versionBehavior", new StringType("default"))
		);

		Bundle returnResource = getClient().operation()
			.onInstance("Library/ReleaseSpecificationLibrary")
			.named("$crmi.release")
			.withParameters(params1)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		// versionBehaviour == 'default' so version should be
		// existingVersion and not the new version provided in
		// the parameters
		assertTrue(releasedLibrary.getVersion().equals(existingVersion));
		List<String> ersdTestArtifactDependencies = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-dxtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-ostc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lotc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-mrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-sdtc|" + existingVersion,
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1063|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.360|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.120|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.362|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.528|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.408|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.409|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1469|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1866|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1906|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.480|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.481|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.761|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1223|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1182|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1181|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1184|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1601|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1600|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1603|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1602|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1082|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1439|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1436|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1435|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1446|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438|2022-10-19",
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf1|0.1.1"
		);
		List<String> ersdTestArtifactComponents = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1"
		);
		List<String> dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		List<String> componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		// check that the released artifact has all the required dependencies
		for(String dependency: ersdTestArtifactDependencies){
			assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
		}
		// and components
		for(String component: ersdTestArtifactComponents){
			assertTrue(componentsOnReleasedArtifact.contains(component));
		}
		assertTrue(ersdTestArtifactDependencies.size() == dependenciesOnReleasedArtifact.size());
		assertTrue(ersdTestArtifactComponents.size() == componentsOnReleasedArtifact.size());
	}

	@Test
	void releaseResource_force_version() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		// Existing version should be "1.2.3";
		String newVersionToForce = "1.2.7.23";

		Parameters params = parameters(
			part("version", new StringType(newVersionToForce)),
			part("versionBehavior", new StringType("force"))
		);

		Bundle returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertTrue(releasedLibrary.getVersion().equals(newVersionToForce));
	}

	@Test
	void releaseResource_propagate_effective_period() {
		loadTransaction("ersd-small-approved-draft-no-child-effective-period.json");
		loadResource("artifactAssessment-search-parameter.json");
		String effectivePeriodToPropagate = "2020-12-11";

		Parameters params = parameters(
			part("version", new StringType("1.2.7.23")),
			part("versionBehavior", new StringType("default"))
		);

		Bundle returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.release")
			.withParameters(params)
			.useHttpGet()
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnResource);
		returnResource.getEntry()
			.stream()
			.map(entry -> entry.getResponse().getLocation())
			.map(location -> {
				switch (location.split("/")[0]) {
					case "ActivityDefinition":
						return getClient().fetchResourceFromUrl(ActivityDefinition.class, location);
					case "Library":
						return getClient().fetchResourceFromUrl(Library.class, location);
					case "Measure":
						return getClient().fetchResourceFromUrl(Measure.class, location);
					case "PlanDefinition":
						return getClient().fetchResourceFromUrl(PlanDefinition.class, location);
					case "ValueSet":
						return getClient().fetchResourceFromUrl(ValueSet.class, location);
					default:
						return null;
				}
			})
			.forEach(resource -> {
				assertNotNull(resource);
				if(!resource.getClass().getSimpleName().equals("ValueSet")){
					KnowledgeArtifactAdapter<MetadataResource> adapter = new KnowledgeArtifactAdapter<>(resource);
					assertTrue(adapter.getEffectivePeriod().hasStart());
					Date start = adapter.getEffectivePeriod().getStart();
					Calendar calendar = new GregorianCalendar();
					calendar.setTime(start);
					int year = calendar.get(Calendar.YEAR);
					int month = calendar.get(Calendar.MONTH) + 1;
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					String startString = year + "-" + month + "-" + day;
					assertTrue(startString.equals(effectivePeriodToPropagate));
				}
			});
	}

	@Test
	void releaseResource_latestFromTx_NotSupported_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		String actualErrorMessage = "";

		Parameters params = parameters(
			part("version", "1.2.3.23"),
			part("versionBehavior", "default"),
			part("latestFromTxServer", new BooleanType(true))
		);

		try {
			getClient().operation()
				.onInstance(specificationLibReference)
				.named("$crmi.release")
				.withParameters(params)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("not yet implemented"));
	}

	@Test
	void release_missing_approvalDate_validation_test() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		String versionData = "1.2.3.23";
		String actualErrorMessage = "";

		Parameters params1 = parameters(
			part("version", versionData),
			part("versionBehavior", "default")
		);

		try {
			getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$crmi.release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("approvalDate"));
	}
	@Test
	void release_version_format_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		for(String version:badVersionList){
			UnprocessableEntityException maybeException = null;
			Parameters params = parameters(
				part("version", new StringType(version)),
				part("versionBehavior", new StringType("force"))
			);
			try {
				getClient().operation()
				.onInstance(specificationLibReference)
				.named("$crmi.release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}
	@Test
	void release_releaseLabel_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String releaseLabel = "release label test";
		Parameters params = parameters(
			part("releaseLabel", new StringType(releaseLabel)),
			part("version", "1.2.3.23"),
			part("versionBehavior", new StringType("default"))
		);
		Bundle returnResource = getClient().operation()
		.onInstance(specificationLibReference)
		.named("$crmi.release")
		.withParameters(params)
		.returnResourceType(Bundle.class)
		.execute();
		assertNotNull(returnResource);
		Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		Optional<Extension> maybeReleaseLabel = releasedLibrary.getExtension().stream().filter(ext -> ext.getUrl().equals(KnowledgeArtifactProcessor.releaseLabelUrl)).findFirst();
		assertTrue(maybeReleaseLabel.isPresent());
		assertTrue(((StringType) maybeReleaseLabel.get().getValue()).getValue().equals(releaseLabel));
	}
	@Test
	void release_version_active_test() {
		loadTransaction("ersd-small-active-bundle.json");
			PreconditionFailedException maybeException = null;
			Parameters params = parameters(
				part("version", new StringType("1.2.3.23")),
				part("versionBehavior", new StringType("force"))
			);
			try {
				getClient().operation()
				.onInstance(specificationLibReference)
				.named("$crmi.release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (PreconditionFailedException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
	}
	@Test
	void release_resource_not_found_test() {
		ResourceNotFoundException maybeException = null;
		Parameters params = parameters(
			part("version", new StringType("1.2.3.23")),
			part("versionBehavior", new StringType("force"))
		);
		try {
			getClient().operation()
			.onInstance("Library/this-resource-does-not-exist")
			.named("$crmi.release")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (ResourceNotFoundException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}
	@Test
	void release_versionBehaviour_format_test() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		List<String> badVersionBehaviors = Arrays.asList(
			"not-a-valid-option",
			null
		);
		for(String versionBehaviour:badVersionBehaviors){
			UnprocessableEntityException maybeException = null;
			Parameters params = parameters(
				part("version", new StringType("1.2.3.23")),
				part("versionBehavior", new StringType(versionBehaviour))
			);
			try {
				getClient().operation()
				.onInstance(specificationLibReference)
				.named("$crmi.release")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}
	@Test
	void release_preserve_vsm_priority_extension() {
		loadTransaction("ersd-small-approved-draft-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		Parameters params = parameters(
			part("version", new StringType("1.2.3.23")),
			part("versionBehavior", new StringType("default"))
		);
		Bundle returnResource =	getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.release")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		assertNotNull(returnResource);
		Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains(specificationLibReference)).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		Optional<RelatedArtifact> maybeRelatedArtifactWithPriorityExtension = releasedLibrary.getRelatedArtifact().stream().filter(ra -> ra.getExtensionByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl) != null).findAny();
		assertTrue(maybeRelatedArtifactWithPriorityExtension.isPresent());
		assertTrue(maybeRelatedArtifactWithPriorityExtension.get().getResource().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|20210526"));
		Extension priority = maybeRelatedArtifactWithPriorityExtension.get().getExtensionByUrl(KnowledgeArtifactProcessor.valueSetPriorityUrl);
		assertTrue(((CodeableConcept) priority.getValue()).getCoding().get(0).getCode().equals("emergent"));
	}
	@Test
	void release_test_artifactComment_updated() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		loadResource("artifactAssessment-search-parameter.json");
		String versionData = "1.2.3";
		Parameters approveParams = parameters(
			part("approvalDate", new DateType(new Date(),TemporalPrecisionEnum.DAY))
		);
		Bundle approvedBundle = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$crmi.approve")
				.withParameters(approveParams)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		Optional<BundleEntryComponent> maybeArtifactAssessment = approvedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeArtifactAssessment.isPresent());
		ArtifactAssessment artifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
		assertTrue(artifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3-draft"));
		Parameters releaseParams = parameters(
			part("version", versionData),
			part("versionBehavior", "default")
		);
		Bundle releasedBundle = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$crmi.release")
				.withParameters(releaseParams)
				.useHttpGet()
				.returnResourceType(Bundle.class)
				.execute();
		Optional<BundleEntryComponent> maybeReleasedArtifactAssessment = releasedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeReleasedArtifactAssessment.isPresent());
		ArtifactAssessment releasedArtifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeReleasedArtifactAssessment.get().getResponse().getLocation());
		assertTrue(releasedArtifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3"));
	}
	@Test
	void reviseOperation_active_test() {
		Library library = (Library) loadResource("ersd-active-library-example.json");
		library.setName("NewSpecificationLibrary");
		String actualErrorMessage = "";
		Parameters params = parameters(part("resource", library));
		try {
			getClient().operation()
				.onServer()
				.named("$crmi.revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
		}
		assertTrue(actualErrorMessage.contains("Current resource status is 'ACTIVE'. Only resources with status of 'draft' can be revised."));
	}

	@Test
	void reviseOperation_draft_test() {
		String newResourceName = "NewSpecificationLibrary";
		Library library = (Library) loadResource("ersd-draft-library-example.json");
		library.setName(newResourceName);
		String errorMessage = "";
		Parameters params = parameters(part("resource", library));
		Library returnResource = null;
		try {
			returnResource = getClient().operation()
				.onServer()
				.named("$crmi.revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			errorMessage = e.getMessage();
		}

		assertTrue(errorMessage.isEmpty());
		assertTrue(returnResource != null);
		assertTrue(returnResource.getName().equals(newResourceName));
	}

	@Test
	void approveOperation_endpoint_id_should_match_target_parameter() {
		loadResource("ersd-active-library-example.json");
		String artifactCommentTarget= "Library/This-Library-Does-Not-Exist|1.0.0";
		Parameters params = parameters(
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget))
		);
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("URL"));
		maybeException = null;
		artifactCommentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|this-version-is-wrong";
		params = parameters(
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget))
		);
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.getMessage().contains("version"));
	}
	@Test
	void approveOperation_should_respect_artifactAssessment_information_type_binding() {
		loadResource("ersd-active-library-example.json");
		String artifactCommentType = "this-type-does-not-exist";
		Parameters params = parameters(
			part("artifactCommentType", artifactCommentType)
		);
		UnprocessableEntityException maybeException = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			maybeException = e;
		}
		assertNotNull(maybeException);
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		loadResource("practitioner-example-for-refs.json");
		Date today = new Date();
		// get today's date in the form "2023-05-11"
 		DateType approvalDate = new DateType(today, TemporalPrecisionEnum.DAY);
		String artifactCommentType = "comment";
		String artifactCommentText = "comment text";
		String artifactCommentTarget= "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0";
		String artifactCommentReference="reference-valid-no-spaces";
		String artifactCommentUser= "Practitioner/sample-practitioner";
		Parameters params = parameters(
			part("approvalDate", approvalDate),
			part("artifactCommentType", artifactCommentType),
			part("artifactCommentText", artifactCommentText),
			part("artifactCommentTarget", new CanonicalType(artifactCommentTarget)),
			part("artifactCommentReference", new CanonicalType(artifactCommentReference)),
			part("artifactCommentUser", new Reference(artifactCommentUser))
		);
		Bundle returnedResource = null;
		returnedResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.approve")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(returnedResource);
		Library lib = getClient().fetchResourceFromUrl(Library.class, specificationLibReference);
		assertNotNull(lib);
		// Ensure Approval Date matches input parameter
		assertTrue(lib.getApprovalDateElement().asStringValue().equals(approvalDate.asStringValue()));
		// match Libray.date to Library.meta.lastUpdated precision before comparing
		lib.getMeta().getLastUpdatedElement().setPrecision(TemporalPrecisionEnum.DAY);
		// Library.date matches the meta.lastUpdated value
		assertTrue(lib.getDateElement().asStringValue().equals(lib.getMeta().getLastUpdatedElement().asStringValue()));
		// Ensure that approval date is NOT before Library.date (see $crmi.release)
		assertFalse(lib.getApprovalDate().before(lib.getDate()));
		// ArtifactAssessment is saved as type Basic, update when we change to OperationOutcome
		// Get the reference from BundleEntry.response.location
		Optional<BundleEntryComponent> maybeArtifactAssessment = returnedResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Basic")).findAny();
		assertTrue(maybeArtifactAssessment.isPresent());
		ArtifactAssessment artifactAssessment = getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
		assertNotNull(artifactAssessment);
		assertTrue(artifactAssessment.isValidArtifactComment());
		assertTrue(artifactAssessment.checkArtifactCommentParams(
			artifactCommentType,
			artifactCommentText,
      specificationLibReference,
			artifactCommentReference,
			artifactCommentTarget,
			artifactCommentUser
		));
	}
	
	@Test
	void packageOperation_should_fail_non_matching_capability() {
		loadTransaction("ersd-active-transaction-capabilities-bundle.json");
		List<String> capabilities = Arrays.asList(
			"computable",
			"publishable",
			"executable"
		);
		// the library contains all three capabilities
		// so we should get an error when trying with
		// any one capability
		for (String capability : capabilities) {
				Parameters params = parameters(
					part("capability", capability)
				);
				PreconditionFailedException maybeException = null;
				try {
					getClient().operation()
					.onInstance(specificationLibReference)
					.named("$crmi.package")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
				} catch (PreconditionFailedException e) {
					maybeException = e;
				}
				assertNotNull(maybeException);
		}
		Parameters allParams = parameters(
			part("capability", "computable"),
			part("capability", "publishable"),
			part("capability", "executable")
		);
		Bundle packaged = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(allParams)
			.returnResourceType(Bundle.class)
			.execute();
		// no error when running the operation with all
		// three capabilities
		assertNotNull(packaged);
	}
	@Test
	void packageOperation_should_apply_check_force_canonicalVersions() {
		loadTransaction("ersd-active-transaction-no-versions.json");
		String versionToUpdateTo = "1.3.1.23";
		Parameters params = parameters(
			part("canonicalVersion", new CanonicalType("http://to-add-missing-version/PlanDefinition/us-ecr-specification|" + versionToUpdateTo)),
			part("canonicalVersion", new CanonicalType("http://to-add-missing-version/ValueSet/dxtc|" + versionToUpdateTo))
		);
		Bundle updatedCanonicalVersionPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		List<MetadataResource> updatedResources = updatedCanonicalVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-add-missing-version"))
			.collect(Collectors.toList());
		assertTrue(updatedResources.size() == 2);
		for (MetadataResource updatedResource: updatedResources) {
			assertTrue(updatedResource.getVersion().equals(versionToUpdateTo));
		}
		params = parameters(
			part("checkCanonicalVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|1.3.1"))
		);
		String correctCheckVersion = "2022-10-19";
		PreconditionFailedException checkCanonicalThrewError = null;
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (PreconditionFailedException e) {
			checkCanonicalThrewError = e;
		}
		assertNotNull(checkCanonicalThrewError);
		params = parameters(
			part("checkCanonicalVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|" + correctCheckVersion))
		);
		Bundle noErrorCheckCanonicalPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<MetadataResource> checkedVersionResource = noErrorCheckCanonicalPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-check-version"))
			.findFirst();
		assertTrue(checkedVersionResource.isPresent());
		assertTrue(checkedVersionResource.get().getVersion().equals(correctCheckVersion));
		String versionToForceTo = "1.1.9.23";
		params = parameters(
			part("forceCanonicalVersion", new CanonicalType("http://to-force-version/Library/rctc|" + versionToForceTo))
		);
		Bundle forcedVersionPackage = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<MetadataResource> forcedVersionResource = forcedVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-force-version"))
			.findFirst();
		assertTrue(forcedVersionResource.isPresent());
		assertTrue(forcedVersionResource.get().getVersion().equals(versionToForceTo));

	}
	@Test
	void packageOperation_should_respect_count_offset() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters countZeroParams = parameters(
			part("count", new IntegerType(0))
		);
		Bundle countZeroBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(countZeroParams)
			.returnResourceType(Bundle.class)
			.execute();
		// when count = 0 only show the total
		assertTrue(countZeroBundle.getEntry().size() == 0);
		assertTrue(countZeroBundle.getTotal() == 5);
		Parameters count2Params = parameters(
			part("count", new IntegerType(2))
		);
		Bundle count2Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(count2Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(count2Bundle.getEntry().size() == 2);
		Parameters count2Offset2Params = parameters(
			part("count", new IntegerType(2)),
			part("offset", new IntegerType(2))
		);
		Bundle count2Offset2Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(count2Offset2Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(count2Offset2Bundle.getEntry().size() == 2);
		Parameters offset4Params = parameters(
			part("offset", new IntegerType(4))
		);
		Bundle offset4Bundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(offset4Params)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offset4Bundle.getEntry().size() == (countZeroBundle.getTotal() - 4));
		Parameters offsetMaxParams = parameters(
			part("offset", new IntegerType(countZeroBundle.getTotal()))
		);
		Bundle offsetMaxBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(offsetMaxParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offsetMaxBundle.getEntry().size() == 0);
		Parameters offsetMaxRandomCountParams = parameters(
			part("offset", new IntegerType(countZeroBundle.getTotal())),
			part("count", new IntegerType(ThreadLocalRandom.current().nextInt(3, 20)))
		);
		Bundle offsetMaxRandomCountBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(offsetMaxRandomCountParams)
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(offsetMaxRandomCountBundle.getEntry().size() == 0);
	}
	@Test
	void packageOperation_should_conditionally_create() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters emptyParams = parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();
		for (BundleEntryComponent component : packagedBundle.getEntry()) {
			String ifNoneExist = component.getRequest().getIfNoneExist();
			String url = ((MetadataResource) component.getResource()).getUrl();
			String version = ((MetadataResource) component.getResource()).getVersion();
			assertTrue(ifNoneExist.equals("url="+url+"&version="+version));
		}
	}
	@Test
	void packageOperation_should_be_aware_of_valueset_priority_extension() {
		loadTransaction("ersd-small-active-bundle.json");
		Parameters emptyParams = parameters();
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(emptyParams)
			.returnResourceType(Bundle.class)
			.execute();
		Optional<ValueSet> shouldBeUpdatedToEmergent = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToEmergent.isPresent());
		Optional<UsageContext> priority = shouldBeUpdatedToEmergent.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(KnowledgeArtifactProcessor.contextTypeUrl) && useContext.getCode().getCode().equals("priority"))
			.findFirst();
		assertTrue(priority.isPresent());
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getCode().equals("emergent"));
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getSystem().equals(KnowledgeArtifactProcessor.contextUrl));

		Optional<ValueSet> shouldBeUpdatedToRoutine = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToRoutine.isPresent());
		Optional<UsageContext> priority2 = shouldBeUpdatedToRoutine.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(KnowledgeArtifactProcessor.contextTypeUrl) && useContext.getCode().getCode().equals("priority"))
			.findFirst();
		assertTrue(priority2.isPresent());
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getCode().equals("routine"));
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getSystem().equals(KnowledgeArtifactProcessor.contextUrl));
	}
	
	@Test
	void packageOperation_should_respect_include() {
		loadTransaction("ersd-small-active-bundle.json");
		Map<String, List<String>> includeOptions = new HashMap<String,List<String>>();
		includeOptions.put("artifact",Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
		includeOptions.put("canonical",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc",
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6"
		));
		includeOptions.put("knowledge",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc"
		));
		includeOptions.put("terminology",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6"
		));
		includeOptions.put("conformance",Arrays.asList());
		includeOptions.put("extensions",Arrays.asList());
		includeOptions.put("profiles",Arrays.asList());
		includeOptions.put("tests",Arrays.asList());
		includeOptions.put("examples",Arrays.asList());
		for (Entry<String, List<String>> includedTypeURLs : includeOptions.entrySet()) {
			Parameters params = parameters(
				part("include", includedTypeURLs.getKey())
			);
			Bundle packaged = getClient().operation()
				.onInstance(specificationLibReference)
				.named("$crmi.package")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			List<MetadataResource> resources = packaged.getEntry().stream()
					.map(entry -> (MetadataResource) entry.getResource())
					.collect(Collectors.toList());
			for (MetadataResource resource: resources) {
				Boolean noExtraResourcesReturned = includedTypeURLs.getValue().stream()
					.anyMatch(url -> url.equals(resource.getUrl()));
				assertTrue(noExtraResourcesReturned);
			}
			for (String url: includedTypeURLs.getValue()) {
				Boolean expectedResourceReturned = resources.stream()
					.anyMatch(resource -> resource.getUrl().equals(url));
				assertTrue(expectedResourceReturned);
			}
		}
	}
	@Test
	void packageOperation_big_bundle() {
		Bundle loadedBundle = (Bundle) loadTransaction("ersd-active-transaction-bundle-example.json");
		Bundle packagedBundle = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$crmi.package")
			.withParameters(parameters())
			.returnResourceType(Bundle.class)
			.execute();
		assertTrue(packagedBundle.getEntry().size() == loadedBundle.getEntry().size());
	}
}

