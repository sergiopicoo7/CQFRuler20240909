package org.opencds.cqf.ruler.sdc.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.sdc.SDCConfig;
import org.opencds.cqf.ruler.sdc.SDCProperties;
import org.opencds.cqf.ruler.test.ITestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
		SDCConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class ExtractProviderIT implements ITestSupport {
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@Autowired
	private DaoRegistry daoRegistry;

	@Autowired
	private SDCProperties mySdcProperties;

	@LocalServerPort
	private int port;

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forCached(FhirVersionEnum.R4);
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);

		mySdcProperties.getExtract().setEndpoint(ourServerBase);
	}

	@Test
	public void testExtract() throws IOException, URISyntaxException {
		String examplePatient = "example_patient.json";
		String exampleQuestionnaire = "questionnaire_1559.json";
		String exampleQR = "questionnaire_response_1558.json";

		loadResource(examplePatient, ourCtx, daoRegistry);
		loadResource(exampleQuestionnaire, ourCtx, daoRegistry);
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) loadResource(exampleQR, ourCtx, daoRegistry);		
		
		Parameters params = new Parameters();
		params.addParameter().setName("questionnaireResponse").setResource(questionnaireResponse);

		Bundle actual = ourClient
				.operation()
				.onType(QuestionnaireResponse.class)
				.named("$extract")
				.withParameters(params)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(actual);

		// Expecting one observation per item
		assertEquals(5, actual.getEntry().size());

		// Ensure the Observations were saved to the local server
		for (Bundle.BundleEntryComponent bec : actual.getEntry()) {
				assertEquals("201 Created", bec.getResponse().getStatus());
		}
	}

	@Test
	public void testExtract_noQuestionnaireReference_throwsException() throws IOException {
		QuestionnaireResponse test = (QuestionnaireResponse) ourCtx.newJsonParser()
				.parseResource(stringFromResource("mypain-questionnaire-response-no-url.json"));

		Parameters params = new Parameters();
		params.addParameter().setName("questionnaireResponse").setResource(test);

		assertThrows(InternalErrorException.class, () -> {
			ourClient.operation().onType(QuestionnaireResponse.class).named("$extract")
					.withParameters(params)
					.returnResourceType(Bundle.class)
					.execute();
		});
	}
}
