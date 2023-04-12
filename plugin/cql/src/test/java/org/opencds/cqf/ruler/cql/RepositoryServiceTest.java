
package org.opencds.cqf.ruler.cql;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.booleanPart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.codePart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.cql.r4.ArtifactCommentExtension;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {RepositoryServiceTest.class, CqlConfig.class},
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
//@TestMethodOrder(MethodOrderer.MethodName.class)

class RepositoryServiceTest extends RestIntegrationTest {
	private final String specificationLibReference = "Library/SpecificationLibrary";
	@Test
	void draftOperation_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		String version = "1.1.1";
		String draftedVersion = version + "-draft";
		Parameters params = parameters(part("version", version) );
		Resource returnResource = getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();

		Bundle returnedBundle = (Bundle) returnResource;
		assertNotNull(returnedBundle);
		Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findAny();
		assertTrue(maybeLib.isPresent());
		Library lib = getClient().fetchResourceFromUrl(Library.class,maybeLib.get().getResponse().getLocation());
		assertNotNull(lib);
		assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
		assertTrue(lib.getVersion().equals(draftedVersion));
		List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()).equals(draftedVersion));
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()).equals(draftedVersion));
	}

	@Test
	void draftOperation_draft_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Parameters params = parameters(part("version", "1.1.1") );
		try {
			getClient().operation()
			.onInstance("Library/DraftSpecificationLibrary")
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			assertNotNull(e);
			assertTrue(e.getMessage().contains("status of 'active'"));
		}
	}
	@Test
	void draftOperation_wrong_id_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Parameters params = parameters(part("version", "1.1.1") );
		try {
			getClient().operation()
			.onInstance("Library/there-is-no-such-id")
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (ResourceNotFoundException e) {
			assertNotNull(e);
		}
	}
	@Test
	void draftOperation_version_conflict_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		loadResource("minimal-draft-to-test-version-conflict.json");
		Parameters params = parameters(part("version", "1.1.1") );
		try {
			getClient().operation()
			.onInstance(specificationLibReference)
			.named("$draft")
			.withParameters(params)
			.returnResourceType(Bundle.class)
			.execute();
		} catch (UnprocessableEntityException e) {
			assertNotNull(e);
			assertTrue(e.getMessage().contains("already exists"));
		}
	}
	@Test
	void draftOperation_version_format_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");
		loadResource("minimal-draft-to-test-version-conflict.json");
		List<String> testValues = Arrays.asList(new String[]{
			"11asd1",
			"1.1.3.1",
			"1.|1.1",
			"1/.1.1",
			"-1.-1.2",
			"1.-1.2",
			"1.1.-2"
		});
		for(String version:testValues){
			Parameters params = parameters(part("version", version) );
			try {
				getClient().operation()
				.onInstance(specificationLibReference)
				.named("$draft")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();
			} catch (UnprocessableEntityException e) {
				assertNotNull(e);
			}
		}
	}
	//@Test
	//void releaseResource_test() {
	//	loadTransaction("ersd-draft-transaction-bundle-example.json");
	//	Library returnResource = getClient().operation()
	//		.onInstance("Library/DraftSpecificationLibrary")
	//		.named("$release")
	//		.withNoParameters(Parameters.class)
	//		.useHttpGet()
	//		.returnResourceType(Library.class)
	//		.execute();

//	@Test
//	void releaseResource_test() {
//		loadTransaction("ersd-release-bundle.json");
//		String versionData = "1234";
//
//		Parameters params1 = parameters(
//			stringPart("version", "1234"),
//			codePart("version-behavior", "default")
//		);
//
//		Library returnResource = getClient().operation()
//			.onInstance("Library/ReleaseSpecificationLibrary")
//			.named("$release")
//			.withParameters(params1)
//			.useHttpGet()
//			.returnResourceType(Library.class)
//			.execute();
//
//		assertNotNull(returnResource);
//	}

	@Test
	void releaseResource_latestFromTx_NotSupported_test() {
		loadTransaction("ersd-release-bundle.json");
		String actualErrorMessage = "";

		Parameters params1 = parameters(
			stringPart("version", "1234"),
			codePart("version-behavior", "default"),
			booleanPart("latest-from-tx-server", true)
		);

		try {
			Library returnResource = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("Support for 'latestFromTxServer' is not yet implemented."));
		}
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
				.named("$revise")
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

	void release_missing_approvalDate_validation_test() {
		loadTransaction("ersd-release-missing-approvalDate-validation-bundle.json");
		String versionData = "1234";
		String actualErrorMessage = "";

		Parameters params1 = parameters(
			stringPart("version", "1234"),
			codePart("version-behavior", "default"),
			booleanPart("latest-from-tx-server", false)
		);

		try {
			Library returnResource = getClient().operation()
				.onInstance("Library/ReleaseSpecificationLibrary")
				.named("$release")
				.withParameters(params1)
				.useHttpGet()
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
		}
	}

	@Test
	void reviseOperation_active_test() {
		Library library = (Library) loadResource("ersd-active-library-example.json");
		library.setName("NewSpecificationLibrary");
		String actualErrorMessage = "";
		Parameters params = parameters(part("resource", library));
		try {
			Library returnResource = getClient().operation()
				.onServer()
				.named("$revise")
				.withParameters(params)
				.returnResourceType(Library.class)
				.execute();
		} catch (Exception e) {
			actualErrorMessage = e.getMessage();
			assertTrue(actualErrorMessage.contains("Current resource status is 'ACTIVE'. Only resources with status of 'draft' can be revised."));
		}
	}

//	@Test
//	void reviseOperation_draft_test() {
//		String newResourceName = "NewSpecificationLibrary";
//		Library library = (Library) loadResource("ersd-draft-library-example.json");
//		library.setName(newResourceName);
//		String errorMessage = "";
//		Parameters params = parameters(part("resource", library));
//		Library returnResource = null;
//		try {
//			returnResource = getClient().operation()
//				.onServer()
//				.named("$revise")
//				.withParameters(params)
//				.returnResourceType(Library.class)
//				.execute();
//		} catch (Exception e) {
//			errorMessage = e.getMessage();
//		}
//
//		assertTrue(errorMessage.isEmpty());
//		assertTrue(returnResource != null);
//		assertTrue(returnResource.getName().equals(newResourceName));
//	}


	@Test
	void packageOperation_library_test() {
		loadTransaction("ersd-package-test-bundle.json");
		String actualMessage = "";
		Bundle returnBundle = null;
		try {
			returnBundle = getClient().operation()
				.onInstance("Library/SpecificationLibrary")
				.named("$package")
				.withNoParameters(Parameters.class)
				.returnResourceType(Bundle.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
			assertTrue(actualMessage.contains("The resource must have a valid id to be packaged."));
		}

		assertNotNull(returnBundle);

		// Test for inclusion of Profile (us-ph-valueset) referenced in Library.dataRequirement[].profile
		assertTrue(returnBundle.getEntry().stream().
			anyMatch(e -> ((MetadataResource)e.getResource()).getUrl().equals("http://ersd.aimsplatform.org/fhir/StructureDefinition/us-ph-valueset")));

		// Test for inclusion of ValueSet referenced in Library.dataRequirement[].codeFilter
		assertTrue(returnBundle.getEntry().stream().anyMatch(e -> ((MetadataResource)e.getResource()).getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438")));

		// Naive test for completeness - based on resource count.
		assertTrue(returnBundle.getEntry().size() == 40);
	}

	@Test
	void approveOperation_twice_appends_artifactComment_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", "documentation")
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Artifact Comment Extension was appended	
		assertTrue(returnedResource.getExtension().size() == 2);
	}

	@Test
	void approveOperation_twice_appends_endorser_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		String endorserName1 = "EndorserName";
		String endorserName2 = "EndorserName2";
		ContactDetail endorser1 = new ContactDetail();
		ContactDetail endorser2 = new ContactDetail();
		endorser1.setName(endorserName1);
		endorser2.setName(endorserName2);
		Parameters params1 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser1)
		);	
		Parameters params2 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser2)
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Library.class)
			.execute();
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Endorser was appended	
		assertTrue(returnedResource.getEndorser().size() == 2);
	}

	@Test
	void approveOperation_twice_updates_endorser_test() {
		loadResource("ersd-active-library-example.json");
		DateType approvalDate = new DateType(DatatypeConverter.parseDate("2022-12-12").getTime());
		String endorserName = "EndorserName";
		ContactDetail endorser = new ContactDetail();
		endorser.setName(endorserName);
		ContactPoint newContact = new ContactPoint();
		String testContactValue = "test";
		newContact.setValue(testContactValue);
		Parameters params1 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser)
		);	
		Library returnedResource = null;
		//once
		getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params1)
			.returnResourceType(Library.class)
			.execute();
			endorser.setTelecom((List<ContactPoint>) Arrays.asList(newContact));
			Parameters params2 = parameters( 
			part("approvalDate", approvalDate),
			part("endorser", endorser)
		);	
			// twice
			returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params2)
			.returnResourceType(Library.class)
			.execute();
		assertNotNull(returnedResource);

		// Endorser was updated	
		assertTrue(returnedResource.getEndorser().size() == 1);
		assertTrue(returnedResource.getEndorser().get(0).getTelecom().get(0).getValue().equals(testContactValue));
	}

	@Test
	void approveOperation_test() {
		loadResource("ersd-active-library-example.json");
		String approvalDateString = "2022-12-12";
		DateType approvalDate = new DateType(DatatypeConverter.parseDate(approvalDateString).getTime());
		String artifactCommentType = "documentation";
		String artifactCommentText = "comment text";
		String artifactCommentTarget= "target";
		String artifactCommentReference="reference-valid-no-spaces";
		String artifactCommentUser="user";
		String endorserName = "EndorserName";
		ContactDetail endorser = new ContactDetail();
		endorser.setName(endorserName);
		Parameters params = parameters( 
			part("approvalDate", approvalDate),
			part("artifactCommentType", artifactCommentType),
			part("artifactCommentText", artifactCommentText),
			part("artifactCommentTarget", artifactCommentTarget),
			part("artifactCommentReference", artifactCommentReference),
			part("artifactCommentUser", artifactCommentUser),
			part("endorser", endorser)
		);	
		Library returnedResource = null;
		returnedResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$approve")
			.withParameters(params)
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnedResource);
				
		// Approval date is correct
		assertTrue(returnedResource.getApprovalDateElement().asStringValue().equals(approvalDateString));
		// match Libray.date to Library.meta.lastUpdated precision before comparing
		returnedResource.getMeta().getLastUpdatedElement().setPrecision(TemporalPrecisionEnum.SECOND);
		// date is correct
		assertTrue(returnedResource.getDateElement().asStringValue().equals(returnedResource.getMeta().getLastUpdatedElement().asStringValue()));
		
		// Artifact Comment Extension is correct	
		Optional<Extension> artifactCommentExtension = returnedResource.getExtension().stream().filter(e -> e.getUrl().equals(ArtifactCommentExtension.ARTIFACT_COMMENT_EXTENSION_URL)).findFirst();
		assertTrue(artifactCommentExtension.isPresent());
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TYPE).getValue().toString().equals(artifactCommentType));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TEXT).getValue().toString().equals(artifactCommentText));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.TARGET).getValue().toString().equals( artifactCommentTarget));
		assertTrue(((UriType) artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.REFERENCE).getValue()).asStringValue().equals(artifactCommentReference));
		assertTrue(artifactCommentExtension.get().getExtensionByUrl(ArtifactCommentExtension.USER).getValue().toString().equals(artifactCommentUser));
		
		// Endorser is correct
		assertTrue(returnedResource.getEndorser().get(0).getName().equals(endorserName));
		
	}
}
