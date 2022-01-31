package org.opencds.cqf.ruler.casereporting.r4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.ruler.provider.DaoRegistryOperationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.BundleUtil;

@Component
public class ProcessMessageProvider extends DaoRegistryOperationProvider {
	private static final Logger logger = LoggerFactory.getLogger(ProcessMessageProvider.class);

	@Operation(name = "$process-message-bundle", idempotent = false)
	public Bundle processMessageBundle(HttpServletRequest theServletRequest, RequestDetails theRequestDetails,
			@OperationParam(name = "content", min = 1, max = 1) @Description(formalDefinition = "The message to process (or, if using asynchronous messaging, it may be a response message to accept)") Bundle theMessageToProcess) {
		logger.info("Validating the Bundle");
		Bundle bundle = theMessageToProcess;
		boolean errorExists = false;
		try {
			OperationOutcome outcome = validateBundle(errorExists, bundle);
			if (!errorExists) {
				IVersionSpecificBundleFactory bundleFactory = this.getFhirContext().newBundleFactory();
				bundle.setId(getUUID());
				bundleFactory.initializeWithBundleResource(bundle);
				Bundle dafBundle = (Bundle) bundleFactory.getResourceBundle();
				dafBundle.setTimestamp(new Date());
				this.getDaoRegistry().getResourceDao(Bundle.class).create(dafBundle);

				MessageHeader messageHeader = null;
				String patientId = null;
				String commId = null;
				List<MessageHeader> headers = BundleUtil.toListOfResourcesOfType(this.getFhirContext(), bundle,
						MessageHeader.class);
				for (MessageHeader mh : headers) {
					messageHeader = mh;
					messageHeader.setId(getUUID());
					Meta meta = messageHeader.getMeta();
					meta.setLastUpdated(new Date());
					messageHeader.setMeta(meta);
				}

				List<Patient> patients = BundleUtil.toListOfResourcesOfType(this.getFhirContext(), bundle, Patient.class);
				for (Patient p : patients) {
					patientId = p.getId();
					p.setId(p.getIdElement().toVersionless());
					this.updateOrCreate(p, theRequestDetails);
				}

				List<Bundle> bundles = BundleUtil.toListOfResourcesOfType(this.getFhirContext(), bundle, Bundle.class);
				for (Bundle b : bundles) {
					patientId = this.processBundle(b, theRequestDetails);
					b.setId(b.getIdElement().toVersionless());
					this.updateOrCreate(b, theRequestDetails);
				}

				for (BundleEntryComponent e : bundle.getEntry()) {
					Resource r = e.getResource();
					if (r == null) {
						continue;
					}

					if (r.fhirType().equals("Bundle") || r.fhirType().equals("MessageHeader")
							|| r.fhirType().equals("Patient")) {
						continue;
					}

					r.setId(r.getIdElement().toVersionless());
					this.updateOrCreate(r, theRequestDetails);
				}

				if (patientId != null) {
					commId = constructAndSaveCommunication(patientId);
				}
				if (messageHeader == null) {
					messageHeader = constructMessageHeaderResource();
					BundleEntryComponent entryComp = new BundleEntryComponent();
					entryComp.setResource(messageHeader);
					dafBundle.addEntry(entryComp);
				}
				if (commId != null) {
					List<Reference> referenceList = new ArrayList<Reference>();
					Reference commRef = new Reference();
					commRef.setReference("Communication/" + commId);
					referenceList.add(commRef);
					messageHeader.setFocus(referenceList);
				}
				return dafBundle;
			} else {
				BundleEntryComponent entryComp = new BundleEntryComponent();
				entryComp.setResource(outcome);
				bundle.addEntry(entryComp);
				return bundle;
			}
		} catch (Exception e) {
			logger.error("Error in Processing the Bundle:" + e.getMessage(), e);
			throw new UnprocessableEntityException("Error in Processing the Bundle", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateOrCreate(Resource r, RequestDetails requestDetails) {
		IFhirResourceDao<IBaseResource> resourceDao = (IFhirResourceDao<IBaseResource>) this.getDaoRegistry()
				.getResourceDao(r.fhirType());
		try {
			if (!r.hasId()) {
				r.setId(getUUID());
			}
			r.setId(r.getIdElement().toVersionless());
			resourceDao.update(r, null, true, true, requestDetails, new TransactionDetails());
		} catch (Exception e) {
			try {
				resourceDao.create(r);
			} catch (Exception ex) {
				logger.error("error creating or updating resource", ex);
			}
		}
	}

	private String processBundle(Bundle bundle, RequestDetails requestDetails) {
		String patientId = null;
		List<Patient> patients = BundleUtil.toListOfResourcesOfType(this.getFhirContext(), bundle, Patient.class);
		for (Patient p : patients) {
			patientId = p.getId();
			p.setId(p.getIdElement().toVersionless());
			this.updateOrCreate(p, requestDetails);
		}

		for (BundleEntryComponent e : bundle.getEntry()) {
			Resource r = e.getResource();
			if (r == null) {
				continue;
			}

			if (r.fhirType().equals("Patient")) {
				continue;
			}

			r.setId(r.getIdElement().toVersionless());
			this.updateOrCreate(r, requestDetails);
		}

		return patientId;
	}

	private OperationOutcome validateBundle(boolean errorExists, Bundle bundle) {
		// FhirValidator validator = this.getFhirContext().newValidator();
		OperationOutcome outcome = new OperationOutcome();
		// // Create a validation module and register it
		// IValidatorModule module = new FhirInstanceValidator(this.getFhirContext());
		// validator.registerValidatorModule(module);
		// ValidationResult result = validator.validateWithResult(bundle);
		// outcome = (OperationOutcome) result.toOperationOutcome();
		// if (outcome.hasIssue()) {
		// List<OperationOutcomeIssueComponent> issueCompList = outcome.getIssue();
		// for (OperationOutcomeIssueComponent issueComp : issueCompList) {
		// if (issueComp.getSeverity().equals(IssueSeverity.ERROR)) {
		// errorExists = true;
		// }
		// }
		// }
		return outcome;
	}

	private MessageHeader constructMessageHeaderResource() {
		String message = "{\"resourceType\": \"MessageHeader\",\"id\": \"messageheader-example-reportheader\",\"meta\": {\"versionId\": \"1\",\"lastUpdated\": \"2020-11-29T02:03:28.045+00:00\",\"profile\": [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-messageheader\"]},\"extension\": [{\"url\": \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-dataEncrypted\",\"valueBoolean\": false},{\"url\":\"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-messageProcessingCategory\",\"valueCode\": \"consequence\"}],\"eventCoding\": {\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\": \"cancer-report-message\"},\"destination\": [{\"name\": \"PHA endpoint\",\"endpoint\": \"http://example.pha.org/fhir\"}],\"source\": {\"name\": \"Healthcare Organization\",\"software\": \"Backend Service App\",\"version\": \"3.1.45.AABB\",\"contact\": {\"system\": \"phone\",\"value\": \"+1 (917) 123 4567\"},\"endpoint\": \"http://example.healthcare.org/fhir\"},\"reason\": {\"coding\": [{\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-triggerdefinition-namedevents\",\"code\": \"encounter-close\"}]}}";
		MessageHeader messageHeader = (MessageHeader) this.getFhirContext().newJsonParser().parseResource(message);
		messageHeader.setId(getUUID());
		return messageHeader;
	}

	private String constructAndSaveCommunication(String patientId) {
		String communication = "{\"resourceType\" : \"Communication\",\"meta\" : {\"versionId\" : \"1\",\"profile\" : [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-communication\"]},\"extension\" : [{\"url\" : \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-responseMessageStatus\",\"valueCodeableConcept\" : {\"coding\" : [{\"system\" :\"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-response-message-processing-status\",\"code\" : \"RRVS1\"}]}}],\"identifier\" : [{\"system\" : \"http://example.pha.org/\",\"value\" : \"12345\"}],\"status\" : \"completed\",\"category\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-response-message\"}]}],\"reasonCode\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-report-message\"}]}]}";
		Communication comm = (Communication) this.getFhirContext().newJsonParser().parseResource(communication);
		String commId = getUUID();
		comm.setId(commId);
		Meta meta = comm.getMeta();
		meta.setLastUpdated(new Date());
		comm.setMeta(meta);
		comm.setSubject(new Reference(patientId));
		comm.setReceived(new Date());
		this.getDaoRegistry().getResourceDao(Communication.class).create(comm);
		return commId;
	}

	public String getUUID() {
		UUID uuid = UUID.randomUUID();
		String randomUUID = uuid.toString();
		return randomUUID;
	}
}
