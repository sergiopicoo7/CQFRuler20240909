
package org.opencds.cqf.ruler.cql;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = { RepositoryServiceTest.class, CqlConfig.class },
	properties = {"hapi.fhir.fhir_version=r4", "hapi.fhir.security.basic_auth.enabled=false"})
class RepositoryServiceTest extends RestIntegrationTest {

	@Test
	void draftOperation_active_test() {
		loadTransaction("ersd-active-transaction-bundle-example.json");

		Resource returnResource = getClient().operation()
			.onInstance("Library/SpecificationLibrary")
			.named("$draft")
			.withNoParameters(Parameters.class)
			.returnResourceType(Library.class)
			.execute();

		Library returnedLibrary = (Library)returnResource;
		assertNotNull(returnedLibrary);
		assertTrue(((Library)returnedLibrary).getStatus() == Enumerations.PublicationStatus.DRAFT);
		List<RelatedArtifact> relatedArtifacts = returnedLibrary.getRelatedArtifact();
		assertTrue(!relatedArtifacts.isEmpty());
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(0).getResource()) == null);
		assertTrue(Canonicals.getVersion(relatedArtifacts.get(1).getResource()) == null);
	}

	@Test
	void draftOperation_draft_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");

		String actualMessage = "";
		try {
			Resource returnResource = getClient().operation()
				.onInstance("Library/DraftSpecificationLibrary")
				.named("$draft")
				.withNoParameters(Parameters.class)
				.returnResourceType(Library.class)
				.execute();
		} catch ( Exception e) {
			actualMessage = e.getMessage();
		}

		assertTrue(actualMessage.contains("Drafts can only be created from artifacts with status of 'active'. Resource 'http://ersd.aimsplatform.org/fhir/Library/DraftSpecificationLibrary' has a status of: DRAFT"));
	}

	@Test
	void releaseResource_test() {
		loadTransaction("ersd-draft-transaction-bundle-example.json");
		Library returnResource = getClient().operation()
			.onInstance("Library/DraftSpecificationLibrary")
			.named("$release")
			.withNoParameters(Parameters.class)
			.useHttpGet()
			.returnResourceType(Library.class)
			.execute();

		assertNotNull(returnResource);
	}
}