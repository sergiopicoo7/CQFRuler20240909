package org.opencds.cqf.ruler.provider;

import org.opencds.cqf.ruler.api.OperationProvider;
import org.opencds.cqf.ruler.behavior.DaoRegistryUser;
import org.opencds.cqf.ruler.behavior.FhirContextUser;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

public class DaoRegistryOperationProvider implements OperationProvider, DaoRegistryUser, FhirContextUser {
	@Autowired
	private DaoRegistry myDaoRegistry;

	@Autowired
	FhirContext myFhirContext;

	public FhirContext getFhirContext() {
		return myFhirContext;
	}

	public DaoRegistry getDaoRegistry() {
		return myDaoRegistry;
	}
}