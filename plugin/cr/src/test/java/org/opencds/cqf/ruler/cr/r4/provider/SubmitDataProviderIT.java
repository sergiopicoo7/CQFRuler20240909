package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

@SpringBootTest(classes = { SubmitDataProviderIT.class }, properties = { "hapi.fhir.fhir_version=r4", })
public class SubmitDataProviderIT extends DaoIntegrationTest {

	@Autowired
	SubmitDataProvider mySubmitDataProvider;

	@Test
	public void testSubmitData() {
		// Create a MR and a resource
		MeasureReport mr = newResource(MeasureReport.class, "test-mr");
		Observation obs = newResource(Observation.class, "test-obs");

		// Submit it
		mySubmitDataProvider.submitData(new SystemRequestDetails(), new IdType("Measure", "test-m"), mr, Lists.newArrayList(obs));

		// Check if they made it to the db
		Observation savedObs = read(obs.getIdElement());
		assertNotNull(savedObs);

		MeasureReport savedMr = read(mr.getIdElement());
		assertNotNull(savedMr);
	}

}