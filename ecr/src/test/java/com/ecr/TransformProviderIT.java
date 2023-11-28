package com.ecr;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.ruler.test.RestIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		TransformConfig.class }, properties = { "hapi.fhir.fhir_version=r4" })
class TransformProviderIT extends RestIntegrationTest {
	@Test
	void testTransformConfig() {
		loadResource("ersd-v1-plandefinition-skeleton.json");
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		Parameters v2BundleParams = new Parameters();
		v2BundleParams.addParameter()
			.setName("bundle")
			.setResource(v2Bundle);
		Bundle v1Bundle = getClient()
				.operation()
				.onServer()
				.named("$ersd-v2-to-v1-transform")
				.withParameters(v2BundleParams)
				.returnResourceType(Bundle.class)
				.execute();

		assertNotNull(v1Bundle);
		List<MetadataResource> entries = v1Bundle.getEntry().stream().map(entry -> (MetadataResource) entry.getResource()).collect(Collectors.toList());
		List<MetadataResource> ersdValueSets = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.ValueSet 
			&& entry.hasMeta() 
			&& entry.getMeta().hasProfile(TransformProperties.ersdVSProfile)).collect(Collectors.toList());
		List<MetadataResource> ersdValueSetLibrary = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.Library 
			&& entry.hasMeta() 
			&& entry.getMeta().hasProfile(TransformProperties.ersdVSLibProfile)).collect(Collectors.toList());
		List<MetadataResource> containsSpecificationLibrary = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.Library && entry.hasMeta() 
			&& (entry.getMeta().hasProfile(TransformProperties.usPHSpecLibProfile) || entry.getUrl().equals("http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary"))).collect(Collectors.toList());
		List<MetadataResource> containsV2PlanDefinition = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.PlanDefinition && entry.hasMeta() 
			&& entry.getMeta().hasProfile(TransformProperties.ersdPlanDefinitionProfile) && !entry.getName().equals("PlanDefinition_eRSD_Skeleton_Instance")).collect(Collectors.toList());
		List<MetadataResource> VSTriggeringUseContextsMissingV1PlanDefinitionReference = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.ValueSet && entry.hasMeta() && entry.getMeta().hasProfile(TransformProperties.ersdVSProfile) 
			&& entry.getUseContext().stream().anyMatch(useContext -> useContext.getCode().getCode().equals("program") && !useContext.getValueReference().getReference().equals("PlanDefinition/plandefinition-ersd-skeleton"))).collect(Collectors.toList());
		List<MetadataResource> containsV1PlanDefinition = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.PlanDefinition && entry.hasMeta() 
			&& entry.getMeta().hasProfile(TransformProperties.ersdPlanDefinitionProfile) && entry.getName().equals("PlanDefinition_eRSD_Skeleton_Instance")).collect(Collectors.toList());
		List<BundleEntryComponent> planDefFullUrlUpdated = v1Bundle.getEntry().stream().filter(entry -> entry.getFullUrl().equals("http://hl7.org/fhir/us/ecr/PlanDefinition/plandefinition-ersd-skeleton|1.2.0.0")).collect(Collectors.toList());
		List<MetadataResource> hasV2TriggeringVSLibUseContexts = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.Library && entry.hasMeta() && entry.getMeta().hasProfile(TransformProperties.ersdVSLibProfile) 
			&& entry.getUseContext().stream().anyMatch(useContext -> 
					  (useContext.getCode().getCode().equals("reporting")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "triggering"))
				 || (useContext.getCode().getCode().equals("specification-type")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "value-set-library")))
		).collect(Collectors.toList());
		List<MetadataResource> hasV2TriggeringVSUseContexts = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.ValueSet && entry.hasMeta() && entry.getMeta().hasProfile(TransformProperties.ersdVSProfile) 
			&& entry.getUseContext().stream().anyMatch(useContext -> 
					  (useContext.getCode().getCode().equals("reporting")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "triggering"))
				 || (useContext.getCode().getCode().equals("priority")
					&& useContext.getValueCodeableConcept().hasCoding(TransformProperties.usPHUsageContext, "routine")))
		).collect(Collectors.toList());
		List<MetadataResource> hasUSPHProfiles = entries.stream().filter(entry -> entry.getResourceType() == ResourceType.PlanDefinition && entry.hasMeta() && entry.getMeta().hasProfile() 
			&& entry.getMeta().getProfile().stream().anyMatch(profile -> profile.getValueAsString().contains("us-ph"))).collect(Collectors.toList());
		List<MetadataResource> hasExperimental = entries.stream().filter(entry -> entry.hasExperimental()).collect(Collectors.toList());
		assertTrue(ersdValueSets.size() > 0);
		assertTrue(ersdValueSetLibrary.size() > 0);
		assertTrue(containsSpecificationLibrary.size() == 0);
		assertTrue(containsV2PlanDefinition.size() == 0);
		assertTrue(containsV1PlanDefinition.size() == 1);
		assertTrue(planDefFullUrlUpdated.size() == 1);
		assertTrue(VSTriggeringUseContextsMissingV1PlanDefinitionReference.size() == 0);
		assertTrue(hasV2TriggeringVSLibUseContexts.size() == 0);
		assertTrue(hasV2TriggeringVSUseContexts.size() == 0);
		assertTrue(hasUSPHProfiles.size() == 0);
		assertTrue(hasExperimental.size() == 0);
	}
	@Test
	void testTransform_alternate_v1_skeleton() {
		PlanDefinition planDef = (PlanDefinition) loadResource("ersd-v1-plandefinition-alternate.json");
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		Parameters v2BundleParams = new Parameters();
		v2BundleParams.addParameter()
			.setName("bundle")
			.setResource(v2Bundle);
		v2BundleParams.addParameter()
			.setName("planDefinition")
			.setResource(planDef);
		Bundle v1Bundle = getClient()
				.operation()
				.onServer()
				.named("$ersd-v2-to-v1-transform")
				.withParameters(v2BundleParams)
				.returnResourceType(Bundle.class)
				.execute();
		List<BundleEntryComponent> bundleContainsAlternatePlanDef = v1Bundle.getEntry().stream().filter(entry -> entry.getFullUrl().equals("http://hl7.org/fhir/us/ecr/PlanDefinition/plandefinition-ersd-skeleton-alternate|test-version")).collect(Collectors.toList());
		assertTrue(bundleContainsAlternatePlanDef.size() == 1);
	}
	@Test
	void testTransform_set_targetVersion() {
		PlanDefinition planDef = (PlanDefinition) loadResource("ersd-v1-plandefinition-testversion.json");
		Bundle v2Bundle = (Bundle) loadResource("ersd-bundle-example.json");
		Parameters v2BundleParams = new Parameters();
		v2BundleParams.addParameter()
			.setName("bundle")
			.setResource(v2Bundle);
		String testVersion = "test-version";
		v2BundleParams.addParameter()
			.setName("targetVersion")
			.setValue(new StringType(testVersion));
		v2BundleParams.addParameter()
			.setName("planDefinition")
			.setResource(planDef);
		Bundle v1Bundle = getClient()
				.operation()
				.onServer()
				.named("$ersd-v2-to-v1-transform")
				.withParameters(v2BundleParams)
				.returnResourceType(Bundle.class)
				.execute();
		List<MetadataResource> resources = v1Bundle.getEntry().stream().map(entry -> (MetadataResource)entry.getResource()).collect(Collectors.toList());
		assertTrue(resources.stream().allMatch(res -> res.getVersion().equals(testVersion)));
	}
}
