package org.opencds.cqf.ruler.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class Libraries {

	private static final Map<FhirVersionEnum, LibraryFunctions> cachedFunctions = new ConcurrentHashMap<>();
	private static final String LIBRARY_RESOURCE_TYPE = "Library";

	private Libraries() {
	}

	static byte[] getContent(IBaseResource library, LibraryFunctions libraryFunctions, String contentType) {
		for (IBase attachment : libraryFunctions.getAttachments().apply(library)) {
			String libraryContentType = libraryFunctions.getContentType().apply(attachment);
			if (libraryContentType != null && libraryContentType.equals(contentType)) {
				byte[] content = libraryFunctions.getContent().apply(attachment);
				if (content != null) {
					return content;
				}
			}
		}

		return null;
	}

	public static byte[] getContent(IBaseResource library, String contentType) {
		checkNotNull(library);
		checkArgument(library.fhirType().equals(LIBRARY_RESOURCE_TYPE));
		checkNotNull(contentType);

		LibraryFunctions libraryFunctions = getFunctions(library);
		return getContent(library, libraryFunctions, contentType);
	}

	static LibraryFunctions getFunctions(IBaseResource library) {
		FhirVersionEnum fhirVersion = library.getStructureFhirVersionEnum();
		return cachedFunctions.computeIfAbsent(fhirVersion, Libraries::getFunctions);
	}

	static LibraryFunctions getFunctions(FhirVersionEnum fhirVersionEnum) {
		FhirContext fhirContext = FhirContext.forCached(fhirVersionEnum);

		Class<? extends IBaseResource> libraryClass = fhirContext.getResourceDefinition(LIBRARY_RESOURCE_TYPE).getImplementingClass();
		Function<IBase, List<IBase>> attachments = Reflections
				.getFunction(libraryClass, "content");
		Function<IBase, String> contentType = Reflections.getPrimitiveFunction(
				fhirContext.getElementDefinition("Attachment").getImplementingClass(), "contentType");
		Function<IBase, byte[]> content = Reflections
				.getPrimitiveFunction(fhirContext.getElementDefinition("Attachment").getImplementingClass(), "data");
		Function<IBase, String> version = Reflections.getVersionFunction(libraryClass);
		return new LibraryFunctions(attachments, contentType, content, version);
	}

	public static String getVersion(IBaseResource library) {
		checkNotNull(library);
		checkArgument(library.fhirType().equals(LIBRARY_RESOURCE_TYPE));

		LibraryFunctions libraryFunctions = getFunctions(library);
		return libraryFunctions.getVersion().apply(library);
	}

	private static Map<String, String> urlsByModelName = ImmutableMap.of(
		"FHIR", "http://hl7.org/fhir",
		"QDM", "urn:healthit-gov:qdm:v5_4");

	// Returns a list of (Model, Version, Url) for the usings in library. The
	// "System" using is excluded.
	public static List<Triple<String, String, String>> getUsingUrlAndVersion(Library.Usings usings) {
		if (usings == null || usings.getDef() == null) {
			return Collections.emptyList();
		}

		List<Triple<String, String, String>> usingDefs = new ArrayList<>();
		for (UsingDef def : usings.getDef()) {

			if (def.getLocalIdentifier().equals("System"))
				continue;

			usingDefs.add(Triple.of(def.getLocalIdentifier(), def.getVersion(),
				urlsByModelName.get(def.getLocalIdentifier())));
		}

		return usingDefs;
	}
}