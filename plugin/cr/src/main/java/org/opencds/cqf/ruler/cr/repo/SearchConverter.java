package org.opencds.cqf.ruler.cr.repo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterOr;
import ca.uhn.fhir.model.api.IQueryParameterType;

public class SearchConverter {
	// hardcoded list from FHIR specs: https://www.hl7.org/fhir/search.html
	private final List<String> searchResultParameters = Arrays.asList("_sort", "_count", "_include",
			"_revinclude", "_summary", "_total", "_elements", "_contained", "_containedType");
	public final Map<String, List<IQueryParameterType>> separatedSearchParameters = new HashMap<>();
	public final Map<String, List<IQueryParameterType>> separatedResultParameters = new HashMap<>();
	public final SearchParameterMap searchParameterMap = new SearchParameterMap();
	public final Map<String, String[]> resultParameters = new HashMap<>();

	void convertParameters(Map<String, List<IQueryParameterType>> theParameters,
			FhirContext theFhirContext) {
		if (theParameters == null) {
			return;
		}
		separateParameterTypes(theParameters);
		convertToSearchParameterMap(separatedSearchParameters);
		convertToStringMap(separatedResultParameters, theFhirContext);
	}

	public void convertToStringMap(@Nonnull Map<String, List<IQueryParameterType>> theParameters,
			@Nonnull FhirContext theFhirContext) {
		for (var entry : theParameters.entrySet()) {
			String[] values = new String[entry.getValue().size()];
			for (int i = 0; i < entry.getValue().size(); i++) {
				values[i] = entry.getValue().get(i).getValueAsQueryToken(theFhirContext);
			}
			resultParameters.put(entry.getKey(), values);
		}
	}

	public void convertToSearchParameterMap(Map<String, List<IQueryParameterType>> theSearchMap) {
		if (theSearchMap == null) {
			return;
		}
		for (var entry : theSearchMap.entrySet()) {
			for (IQueryParameterType value : entry.getValue()) {
				setParameterTypeValue(entry.getKey(), value);
			}
		}
	}

	public <T> void setParameterTypeValue(@Nonnull String theKey, @Nonnull T theParameterType) {
		if (isOrList(theParameterType)) {
			searchParameterMap.add(theKey, (IQueryParameterOr<?>) theParameterType);
		} else if (isAndList(theParameterType)) {
			searchParameterMap.add(theKey, (IQueryParameterAnd<?>) theParameterType);
		} else {
			searchParameterMap.add(theKey, (IQueryParameterType) theParameterType);
		}
	}

	public void separateParameterTypes(
			@Nonnull Map<String, List<IQueryParameterType>> theParameters) {
		for (var entry : theParameters.entrySet()) {
			if (isSearchResultParameter(entry.getKey())) {
				separatedResultParameters.put(entry.getKey(), entry.getValue());
			} else {
				separatedSearchParameters.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public boolean isSearchResultParameter(String theParameterName) {
		return searchResultParameters.contains(theParameterName);
	}

	public <T> boolean isOrList(@Nonnull T theParameterType) {
		return IQueryParameterOr.class.isAssignableFrom(theParameterType.getClass());
	}

	public <T> boolean isAndList(@Nonnull T theParameterType) {
		return IQueryParameterAnd.class.isAssignableFrom(theParameterType.getClass());
	}
}
