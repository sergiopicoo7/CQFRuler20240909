package org.opencds.cqf.ruler.common.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;


public interface IdCreator extends FhirContextUser {

	default <T extends IIdType> T newId(String theResourceName, String theResourceId) {
		checkNotNull(theResourceName);
		checkNotNull(theResourceId);

		return Ids.newId(getFhirContext(), theResourceName, theResourceId);
	}

	default <T extends IIdType> T newId(String theResourceId) {
		checkNotNull(theResourceId);
	
		return Ids.newId(getFhirContext(), theResourceId);
	}
}
