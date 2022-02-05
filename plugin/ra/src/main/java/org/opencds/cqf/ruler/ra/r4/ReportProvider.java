package org.opencds.cqf.ruler.ra.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.behavior.IdCreator;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.opencds.cqf.ruler.utility.Operations;
import org.opencds.cqf.ruler.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;

public class ReportProvider extends DaoRegistryOperationProvider implements IdCreator {
	private static final Logger ourLog = LoggerFactory.getLogger(ReportProvider.class);
	/**
	 * Implements the <a href=
	 * "https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html">$report</a>
	 * operation found in the
	 * <a href="https://build.fhir.org/ig/HL7/davinci-ra/index.html">Da Vinci Risk
	 * Adjustment IG</a>.
	 * 
	 * @param requestDetails metadata about the current request being processed.
	 *                       Generally auto-populated by the HAPI FHIR server
	 *                       framework.
	 * @param periodStart    the start of the clinical evaluation period
	 * @param periodEnd      the end of the clinical evaluation period
	 * @param subject        a Patient or Patient Group
	 * @return a Parameters with Bundles of MeasureReports and evaluatedResource
	 *         Resources
	 */
	@Description(shortDefinition = "$report", value = "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-report.html\">$report</a> operation found in the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/index.html\">Da Vinci Risk Adjustment IG</a>.")

	@Operation(name = "$report", idempotent = true, type = MeasureReport.class)
	public Parameters report(
			RequestDetails requestDetails,
			@OperationParam(name = "periodStart", min = 1, max = 1) String periodStart,
			@OperationParam(name = "periodEnd", min = 1, max = 1) String periodEnd,
			@OperationParam(name = "subject", min = 1, max = 1) String subject) throws FHIRException {

		Period period = validateParameters(periodStart, periodEnd, subject);
		Parameters result = initializeParametersResult(subject);
		List<Patient> patients = getPatientListFromSubject(subject);

		(patients)
				.forEach(
						patient -> {
							Parameters.ParametersParameterComponent patientParameter = patientReport(patient, period,
									requestDetails.getFhirServerBase());
							result.addParameter(patientParameter);
						});

		return result;
	}

	private Period validateParameters(String periodStart, String periodEnd, String subject) {
		if (periodStart == null) {
			throw new IllegalArgumentException("Parameter 'periodStart' is required.");
		}
		if (periodEnd == null) {
			throw new IllegalArgumentException("Parameter 'periodEnd' is required.");
		}
		Date periodStartDate = Operations.resolveRequestDate(periodStart, true);
		Date periodEndDate = Operations.resolveRequestDate(periodEnd, false);
		if (periodStartDate.after(periodEndDate)) {
			throw new IllegalArgumentException("Parameter 'periodStart' must be before 'periodEnd'.");
		}

		if (subject == null) {
			throw new IllegalArgumentException("Parameter 'subject' is required.");
		}
		if (!subject.startsWith("Patient/") && !subject.startsWith("Group/")) {
			throw new IllegalArgumentException(
					"Parameter 'subject' must be in the format 'Patient/[id]' or 'Group/[id]'.");
		}

		return new Period().setStart(periodStartDate).setEnd(periodEndDate);
	}

	private Parameters initializeParametersResult(String subject) {
		Parameters result = new Parameters();
		result.setId(subject.replace("/", "-") + "-report");

		return result;
	}

	private static final String PATIENT_REPORT_PROFILE_URL = "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-bundle";

	private Parameters.ParametersParameterComponent patientReport(Patient thePatient, Period thePeriod,
			String serverBase) {

		String patientId = thePatient.getIdElement().getIdPart();
		final Map<IIdType, IAnyResource> bundleEntries = new HashMap<>();
		bundleEntries.put(thePatient.getIdElement(), thePatient);

		ReferenceParam subjectParam = new ReferenceParam(patientId);
		search(MeasureReport.class, Searches.byParam("subject", subjectParam)).getAllResourcesTyped()
				.forEach(measureReport -> {

					if (measureReport.getPeriod().getEnd().before(thePeriod.getStart())
							|| measureReport.getPeriod().getStart().after(thePeriod.getEnd())) {
						return;
					}

					bundleEntries.putIfAbsent(measureReport.getIdElement(), measureReport);

					getEvaluatedResources(measureReport).forEach(resource ->
						bundleEntries.putIfAbsent(resource.getIdElement(), resource));
				});

		Bundle patientReportBundle = new Bundle();
		patientReportBundle.setMeta(new Meta().addProfile(PATIENT_REPORT_PROFILE_URL));
		patientReportBundle.setType(Bundle.BundleType.COLLECTION);
		patientReportBundle.setTimestamp(new Date());
		patientReportBundle.setId(patientId + "-report");
		patientReportBundle.setIdentifier(
				new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + UUID.randomUUID().toString()));

		bundleEntries.entrySet().forEach(resource ->
			patientReportBundle.addEntry(
					new Bundle.BundleEntryComponent()
							.setResource((Resource) resource.getValue())
							.setFullUrl(Operations.getFullUrl(serverBase, resource.getValue().fhirType(),
									resource.getValue().getIdElement().getIdPart()))));

		Parameters.ParametersParameterComponent patientParameter = new Parameters.ParametersParameterComponent();
		patientParameter.setResource(patientReportBundle);
		patientParameter.setId(thePatient.getIdElement().getIdPart() + "-report");
		patientParameter.setName("return");

		return patientParameter;
	}

	// TODO: this might be useful to make a utility
	private List<IAnyResource> getEvaluatedResources(MeasureReport report) {
		List<IAnyResource> resources = new ArrayList<>();
		for (Reference evaluatedResource : report.getEvaluatedResource()) {
			IIdType theEvaluatedId = evaluatedResource.getReferenceElement();
			IBaseResource resourceBase = read(theEvaluatedId);
			if (resourceBase instanceof Resource) {
				Resource resource = (Resource) resourceBase;
				resources.add(resource);
			}
		}
		return resources;

	}

	// TODO: replace this with version from the evaluator?
	private Patient ensurePatient(String patientRef) {
		Patient patient = read(newId(patientRef));
		if (patient == null) {
			throw new RuntimeException("Could not find Patient: " + patientRef);
		}
		return patient;
	}

	// TODO: replace this with version from the evaluator?
	private List<Patient> getPatientListFromSubject(String subject) {
		if (subject.startsWith("Patient/")) {
			Patient patient = ensurePatient(subject);
			return Collections.singletonList(patient);
		} else if (subject.startsWith("Group/")) {
			return getPatientListFromGroup(subject);
		}

		ourLog.info("Subject member was not a Patient or a Group, so skipping. \n{}", subject);
		return Collections.emptyList();
	}

	// TODO: replace this with version from the evaluator?
	private List<Patient> getPatientListFromGroup(String subjectGroupId) {
		List<Patient> patientList = new ArrayList<>();

		Group group = read(newId(subjectGroupId));
		if (group == null) {
			throw new RuntimeException("Could not find Group: " + subjectGroupId);
		}

		group.getMember().forEach(member -> {
			Reference reference = member.getEntity();
			if (reference.getReferenceElement().getResourceType().equals("Patient")) {
				Patient patient = ensurePatient(reference.getReference());
				patientList.add(patient);
			} else if (reference.getReferenceElement().getResourceType().equals("Group")) {
				patientList.addAll(getPatientListFromGroup(reference.getReference()));
			} else {
				ourLog.info("Group member was not a Patient or a Group, so skipping. \n{}",
						reference.getReference());
			}
		});

		return patientList;
	}
}