package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.ruler.Application;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.devtools.DevToolsConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class,
        CrConfig.class, CqlConfig.class, DevToolsConfig.class  }, properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "spring.batch.job.enabled=false",
            "hapi.fhir.fhir_version=r4",
				"hapi.fhir.allow_external_references=true",
				"hapi.fhir.enforce_referential_integrity_on_write=false"
})
public class ActivityDefinitionApplyProviderIT extends RestIntegrationTest {

	@Autowired
	private ActivityDefinitionApplyProvider activityDefinitionApplyProvider;

	private Map<String, IBaseResource> activityDefinitions;

	@BeforeEach
	public void setup() throws Exception {
		 activityDefinitions = uploadTests("activitydefinition");
	}

	@Test
	public void testActivityDefinitionApply() throws Exception {
		 DomainResource activityDefinition = (DomainResource) activityDefinitions.get("opioidcds-risk-assessment-request");
		 // Patient First
		 Map<String, IBaseResource> resources = uploadTests("test/activitydefinition/Patient");
		 IBaseResource patient = resources.get("ExamplePatient");
		 Resource applyResult = activityDefinitionApplyProvider.apply(new SystemRequestDetails(), activityDefinition.getIdElement(), patient.getIdElement().getIdPart(), null, null, null, null, null, null, null, null);
			assertTrue(applyResult instanceof ServiceRequest);
			assertEquals("454281000124100", ((ServiceRequest) applyResult).getCode().getCoding().get(0).getCode());
		 }
}
