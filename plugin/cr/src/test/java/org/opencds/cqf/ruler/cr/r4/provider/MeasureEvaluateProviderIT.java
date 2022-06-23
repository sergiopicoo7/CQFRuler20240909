package org.opencds.cqf.ruler.cr.r4.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newParameters;
import static org.opencds.cqf.ruler.utility.r4.Parameters.newPart;

import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.cql.CqlConfig;
import org.opencds.cqf.ruler.cr.CrConfig;
import org.opencds.cqf.ruler.security.SecurityConfig;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {
		MeasureEvaluateProviderIT.class,
		CrConfig.class, CqlConfig.class, SecurityConfig.class }, properties = {
				"hapi.fhir.fhir_version=r4", "hapi.fhir.security.enabled=true" })

public class MeasureEvaluateProviderIT extends RestIntegrationTest {

	@Test
	public void testMeasureEvaluate() throws Exception {
		String bundleAsText = stringFromResource("Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Parameters params = newParameters(
				newPart("periodStart", "2019-01-01"),
				newPart("periodEnd", "2020-01-01"),
				newPart("reportType", "individual"),
				newPart("subject", "Patient/numer-EXM104"),
				newPart("lastReceivedOn", "2019-12-12"));

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}

	@Test
	public void testMeasureEvaluateWithTerminologyEndpoint() throws Exception {
		String bundleAsText = stringFromResource("Exm104FhirR4MeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		getClient().operation().onInstance(new IdType("ValueSet",
				"2.16.840.1.114222.4.11.3591")).named("expand")
				.withNoParameters(Parameters.class).execute();

		String terminologyAsText = stringFromResource("Endpoint.json");

		Endpoint terminologyEndpointValid = (Endpoint) getFhirContext().newJsonParser().parseResource(terminologyAsText);
		terminologyEndpointValid.setAddress(this.getServerBase());

		Endpoint terminologyEndpointInvalid = (Endpoint) getFhirContext().newJsonParser().parseResource(terminologyAsText);
		terminologyEndpointInvalid.setAddress("https://tx.nhsnlink.org/fhir234");

		Parameters params = newParameters(
			newPart("periodStart", "2019-01-01"),
			newPart("periodEnd", "2020-01-01"),
			newPart("reportType", "individual"),
			newPart("subject", "Patient/numer-EXM104"),
			newPart("lastReceivedOn", "2019-12-12"),
			newPart("terminologyEndpoint", terminologyEndpointValid));

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);

		Parameters paramsWithInvalidTerminology = newParameters(
			newPart("periodStart", "2019-01-01"),
			newPart("periodEnd", "2020-01-01"),
			newPart("reportType", "individual"),
			newPart("subject", "Patient/numer-EXM104"),
			newPart("lastReceivedOn", "2019-12-12"),
			newPart("terminologyEndpoint", terminologyEndpointInvalid));

		Exception ex = assertThrows(Exception.class, () -> {
			getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$evaluate-measure")
				.withParameters(paramsWithInvalidTerminology)
				.returnResourceType(MeasureReport.class)
				.execute();
		});

		//prints : HTTP 500 : Unexpected exception caught during execution: ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException: HTTP 404
		//instead of : Failed to call access method: org.opencds.cqf.cql.engine.exception.CqlException: Unexpected exception caught during execution: ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException: HTTP 404
		assertEquals("HTTP 500 : Unexpected exception caught during execution: ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException: HTTP 404 ", ex.getMessage());
	}

	@Test
	public void testMeasureEvaluateWithAdditionalData() throws Exception {
		String mainBundleAsText = stringFromResource("Exm104FhirR4MeasurePartBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(mainBundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		String additionalBundleAsText = stringFromResource("Exm104FhirR4MeasureAdditionalData.json");
		Bundle additionalData = (Bundle) getFhirContext().newJsonParser().parseResource(additionalBundleAsText);

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("subject"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM104"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));
		params.addParameter().setName("additionalData").setResource(additionalData);

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM104-8.2.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);
	}

	private void runWithPatient(String measureId, String patientId, int initialPopulationCount, int denominatorCount,
			int denominatorExclusionCount, int numeratorCount, boolean enrolledDuringParticipationPeriod,
			String participationPeriod) {
		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2022-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2022-12-31"));
		params.addParameter().setName("reportType").setValue(new StringType("individual"));
		params.addParameter().setName("subject").setValue(new StringType(patientId));

		MeasureReport returnMeasureReport = getClient().operation().onInstance(new IdType("Measure", measureId))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);

		for (MeasureReport.MeasureReportGroupPopulationComponent population : returnMeasureReport.getGroupFirstRep()
				.getPopulation()) {
			switch (population.getCode().getCodingFirstRep().getCode()) {
				case "initial-population":
					assertEquals(initialPopulationCount, population.getCount());
					break;
				case "denominator":
					assertEquals(denominatorCount, population.getCount());
					break;
				case "denominator-exclusion":
					assertEquals(denominatorExclusionCount, population.getCount());
					break;
				case "numerator":
					assertEquals(numeratorCount, population.getCount());
					break;
			}
		}

		Observation enrolledDuringParticipationPeriodObs = null;
		Observation participationPeriodObs = null;
		for (Resource r : returnMeasureReport.getContained()) {
			if (r instanceof Observation) {
				Observation o = (Observation) r;
				if (o.getCode().getText().equals("Enrolled During Participation Period")) {
					enrolledDuringParticipationPeriodObs = o;
					continue;
				} else if (o.getCode().getText().equals("Participation Period")) {
					participationPeriodObs = o;
					continue;
				}
			}
		}

		assertNotNull(enrolledDuringParticipationPeriodObs);
		assertEquals(Boolean.toString(enrolledDuringParticipationPeriod).toLowerCase(),
				enrolledDuringParticipationPeriodObs.getValueCodeableConcept().getCodingFirstRep().getCode());

		assertNotNull(participationPeriodObs);
		assertEquals(participationPeriod, participationPeriodObs.getValueCodeableConcept().getCodingFirstRep().getCode());
	}

	@Test
	public void testBCSEHEDISMY2022() throws Exception {
		String bundleAsText = stringFromResource("BCSEHEDISMY2022-bundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-5", 0, 0, 0, 0, false,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-7", 1, 1, 0, 0, true,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-9", 0, 0, 0, 0, true,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-21", 1, 0, 1, 0, true,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-23", 1, 1, 0, 0, true,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
		runWithPatient("BCSEHEDISMY2022", "Patient/Patient-65", 1, 1, 0, 1, true,
				"Interval[2020-10-01T00:00:00.000, 2022-12-31T23:59:59.999]");
	}

	@Test
	public void testClientNonPatientBasedMeasureEvaluate() throws Exception {
		String bundleAsText = stringFromResource("ClientNonPatientBasedMeasureBundle.json");
		Bundle bundle = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsText);
		getClient().transaction().withBundle(bundle).execute();

		Measure measure = getClient().read().resource(Measure.class).withId("InitialInpatientPopulation").execute();
		assertNotNull(measure);

		Parameters params = new Parameters();
		params.addParameter().setName("periodStart").setValue(new StringType("2019-01-01"));
		params.addParameter().setName("periodEnd").setValue(new StringType("2020-01-01"));
		params.addParameter().setName("reportType").setValue(new StringType("subject"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/97f27374-8a5c-4aa1-a26f-5a1ab03caa47"));

		MeasureReport returnMeasureReport = getClient().operation()
				.onInstance(new IdType("Measure", "InitialInpatientPopulation"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReport);

		String populationName = "initial-population";
		int expectedCount = 2;

		Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = returnMeasureReport.getGroup().get(0)
				.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding()
						&& x.getCode().getCoding().get(0).getCode().equals(populationName))
				.findFirst();

		assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));
		assertEquals(population.get().getCount(), expectedCount,
				String.format("expected count for population \"%s\" did not match", populationName));
	}

	@Disabled("The cql/elm in the Bundles is incorrect. It references ValueSets by localhost url, which is not valid")
	@Test
	public void testMeasureEvaluateMultiVersion() throws Exception {
		String bundleAsTextVersion7 = stringFromResource("multiversion/EXM124-7.0.000-bundle.json");
		String bundleAsTextVersion9 = stringFromResource("multiversion/EXM124-9.0.000-bundle.json");
		Bundle bundleVersion7 = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsTextVersion7);
		Bundle bundleVersion9 = (Bundle) getFhirContext().newJsonParser().parseResource(bundleAsTextVersion9);
		getClient().transaction().withBundle(bundleVersion7).execute();
		getClient().transaction().withBundle(bundleVersion9).execute();
		Parameters params = new Parameters();
		params.addParameter().setName("reportType").setValue(new StringType("individual"));
		params.addParameter().setName("subject").setValue(new StringType("Patient/numer-EXM124"));
		params.addParameter().setName("lastReceivedOn").setValue(new StringType("2019-12-12"));

		MeasureReport returnMeasureReportVersion7 = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM124-7.0.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReportVersion7);

		MeasureReport returnMeasureReportVersion9 = getClient().operation()
				.onInstance(new IdType("Measure", "measure-EXM124-9.0.000"))
				.named("$evaluate-measure")
				.withParameters(params)
				.returnResourceType(MeasureReport.class)
				.execute();

		assertNotNull(returnMeasureReportVersion9);

	}

}
