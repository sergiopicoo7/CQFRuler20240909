package org.opencds.cqf.ruler.plugin.cql;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.fhir.retrieve.SearchParamFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterMap;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * This class provides an implementation of the cql-engine's RetrieveProvider interface which is used for loading
 * data during CQL evaluation.
 */
public class JpaFhirRetrieveProvider extends SearchParamFhirRetrieveProvider {

    private static final Logger logger = LoggerFactory.getLogger(JpaFhirRetrieveProvider.class);

    private final DaoRegistry myDaoRegistry;
	 private final RequestDetails myRequestDetails;

	 public JpaFhirRetrieveProvider(DaoRegistry theDaoRegistry, SearchParameterResolver theSearchParameterResolver) {
		this(theDaoRegistry, theSearchParameterResolver, null);
  }

    public JpaFhirRetrieveProvider(DaoRegistry registry, SearchParameterResolver searchParameterResolver, RequestDetails requestDetails) {
        super(searchParameterResolver);
        this.myDaoRegistry = registry;
		  this.myRequestDetails = requestDetails;
    }

    @Override
    protected Iterable<Object> executeQueries(String dataType, List<SearchParameterMap> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> objects = new ArrayList<>();
        for (SearchParameterMap map : queries) {
            objects.addAll(executeQuery(dataType, map));
        }

        return objects;
    }

    protected Collection<Object> executeQuery(String dataType, SearchParameterMap map) {
        // TODO: Once HAPI breaks this out from the server dependencies
        // we can include it on its own.
        ca.uhn.fhir.jpa.searchparam.SearchParameterMap hapiMap = ca.uhn.fhir.jpa.searchparam.SearchParameterMap.newSynchronous();
        try {

            Method[] methods = hapiMap.getClass().getDeclaredMethods();
            List<Method> methodList = Arrays.asList(methods);
            List<Method> puts = methodList.stream().filter(x -> x.getName().equals("put")).collect(Collectors.toList());
            Method method = puts.get(0);
            method.setAccessible(true);

            for (Map.Entry<String, List<List<IQueryParameterType>>> entry : map.entrySet()) {
                method.invoke(hapiMap, entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            logger.warn("Error converting search parameter map", e);
        }

        IFhirResourceDao<?> dao = this.myDaoRegistry.getResourceDao(dataType);

        IBundleProvider bundleProvider = dao.search(hapiMap, myRequestDetails);
        if (bundleProvider.size() == null) {
            return resolveResourceList(bundleProvider.getAllResources());
        }
        if (bundleProvider.size() == 0) {
            return new ArrayList<>();
        }
        List<IBaseResource> resourceList = bundleProvider.getAllResources();
        return resolveResourceList(resourceList);
    }

    public Collection<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>(resourceList.size());
		  ret.addAll(resourceList);
        return ret;
    }
}