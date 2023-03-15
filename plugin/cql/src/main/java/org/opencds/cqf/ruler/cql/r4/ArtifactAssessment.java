package org.opencds.cqf.ruler.cql.r4;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.ResourceDef;


@ResourceDef(name="ArtifactAssessment", id="ArtifactAssessment", profile = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessment")
public class ArtifactAssessment extends Basic {
	public enum ArtifactAssessmentContentInformationType {
		/**
		 * A comment on the artifact
		 */
		COMMENT,
		/**
		 * A classifier of the artifact
		 */
		CLASSIFIER,
		/**
		 * A rating of the artifact
		 */
		RATING,
		/**
		 * A container for multiple components
		 */
		CONTAINER,
		/**
		 * A response to a comment
		 */
		RESPONSE,
		/**
		 * A change request for the artifact
		 */
		CHANGEREQUEST,
		/**
		 * added to help the parsers with the generic types
		 */
		NULL;

		public static ArtifactAssessmentContentInformationType fromCode(String codeString) throws FHIRException {
			if (codeString == null || "".equals(codeString))
				return null;
			if ("comment".equals(codeString))
				return COMMENT;
			if ("classifier".equals(codeString))
				return CLASSIFIER;
			if ("rating".equals(codeString))
				return RATING;
			if ("container".equals(codeString))
				return CONTAINER;
			if ("response".equals(codeString))
				return RESPONSE;
			if ("change-request".equals(codeString))
				return CHANGEREQUEST;
			if (Configuration.isAcceptInvalidEnums())
				return null;
			else
				throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
		}

		public String toCode() {
			switch (this) {
				case COMMENT:
					return "comment";
				case CLASSIFIER:
					return "classifer";
				case RATING:
					return "rating";
				case CONTAINER:
					return "container";
				case RESPONSE:
					return "response";
				case CHANGEREQUEST:
					return "change-request";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getSystem() {
			switch (this) {
				case COMMENT:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case CLASSIFIER:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case RATING:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case CONTAINER:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case RESPONSE:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case CHANGEREQUEST:
					return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDefinition() {
			switch (this) {
				case COMMENT:
					return "A comment on the artifact.";
				case CLASSIFIER:
					return "A classifier of the artifact.";
				case RATING:
					return "A rating  of the artifact.";
				case CONTAINER:
					return "A container for multiple components.";
				case RESPONSE:
					return "A response to a comment.";
				case CHANGEREQUEST:
					return "A change request for the artifact.";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDisplay() {
			switch (this) {
				case COMMENT:
					return "Comment";
				case CLASSIFIER:
					return "Classifer";
				case RATING:
					return "Rating";
				case CONTAINER:
					return "Container";
				case RESPONSE:
					return "Response";
				case CHANGEREQUEST:
					return "Change Request";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

	}

	public static class ArtifactAssessmentContentInformationTypeEnumFactory implements EnumFactory<ArtifactAssessmentContentInformationType> {
		public ArtifactAssessmentContentInformationType fromCode(String codeString) throws IllegalArgumentException {
			if (codeString == null || "".equals(codeString))
				if (codeString == null || "".equals(codeString))
					return null;
			if ("comment".equals(codeString))
				return ArtifactAssessmentContentInformationType.COMMENT;
			if ("classifier".equals(codeString))
				return ArtifactAssessmentContentInformationType.CLASSIFIER;
			if ("rating".equals(codeString))
				return ArtifactAssessmentContentInformationType.RATING;
			if ("container".equals(codeString))
				return ArtifactAssessmentContentInformationType.CONTAINER;
			if ("response".equals(codeString))
				return ArtifactAssessmentContentInformationType.RESPONSE;
			if ("change-request".equals(codeString))
				return ArtifactAssessmentContentInformationType.CHANGEREQUEST;
			throw new IllegalArgumentException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public Enumeration<ArtifactAssessmentContentInformationType> fromType(Base code) throws FHIRException {
			if (code == null)
				return null;
			if (code.isEmpty())
				return new Enumeration<ArtifactAssessmentContentInformationType>(this);
			String codeString = ((PrimitiveType) code).asStringValue();
			if (codeString == null || "".equals(codeString))
				return null;
			if ("comment".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.COMMENT);
				if ("classifier".equals(codeString))
					return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.CLASSIFIER);
				if ("rating".equals(codeString))
					return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.RATING);
				if ("container".equals(codeString))
					return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.CONTAINER);
				if ("response".equals(codeString))
					return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.RESPONSE);
				if ("change-request".equals(codeString))
					return new Enumeration<ArtifactAssessmentContentInformationType>(this, ArtifactAssessmentContentInformationType.CHANGEREQUEST);

			throw new FHIRException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public String toCode(ArtifactAssessmentContentInformationType code) {
			if (code == ArtifactAssessmentContentInformationType.COMMENT)
				return "comment";
			if (code == ArtifactAssessmentContentInformationType.CLASSIFIER)
				return "classifier";
			if (code == ArtifactAssessmentContentInformationType.RATING)
				return "rating";
			if (code == ArtifactAssessmentContentInformationType.CONTAINER)
				return "container";
			if (code == ArtifactAssessmentContentInformationType.RESPONSE)
				return "response";
			if (code == ArtifactAssessmentContentInformationType.CHANGEREQUEST)
				return "change-request";
			return "?";
		}

		public String toSystem(ArtifactAssessmentContentInformationType code) {
			return code.getSystem();
		}
	}
	
	public enum ArtifactAssessmentContentClassifier {
		/**
		 * High quality evidence.
		 */
		HIGH,
		/**
		 * Moderate quality evidence.
		 */
		MODERATE,
		/**
		 * Low quality evidence.
		 */
		LOW,
		/**
		 * Very low quality evidence
		 */
		VERY_LOW,
		/**
		 * No serious concern.
		 */
		NO_CONCERN,
		/**
		 * Serious concern.
		 */
		SERIOUS_CONCERN,
		/**
		 * Very serious concern.
		 */
		VERY_SERIOUS_CONCERN,
		/**
		 * Extremely serious concern.
		 */
		EXTREMELY_SERIOUS_CONCERN,
		/**
		 * Possible reason for increasing quality rating was checked and found to be present.
		 */
		PRESENT,
		/**
		 * Possible reason for increasing quality rating was checked and found to be absent.
		 */
		ABSENT,
		/**
		 * No change to quality rating.
		 */
		NO_CHANGE,
		/**
		 * Reduce quality rating by 1.
		 */
		DOWNCODE1,
		/**
		 * Reduce quality rating by 2.
		 */
		DOWNCODE2,
		/**
		 * Reduce quality rating by 3.
		 */
		DOWNCODE3,
		/**
		 * Increase quality rating by 1.
		 */
		UPCODE1,
		/**
		 * Increase quality rating by 2
		 */
		UPCODE2,
		/**
		 * added to help the parsers with the generic types
		 */
		NULL;

		public static ArtifactAssessmentContentClassifier fromCode(String codeString) throws FHIRException {
			if (codeString == null || "".equals(codeString))
				return null;
			if ("high".equals(codeString))
				return HIGH;
			if ("moderate".equals(codeString))
				return MODERATE;
			if ("low".equals(codeString))
				return LOW;
			if ("very-low".equals(codeString))
				return VERY_LOW;
			if ("no-concern".equals(codeString))
				return NO_CONCERN;
			if ("serious-concern".equals(codeString))
				return SERIOUS_CONCERN;
			if ("very-serious-concern".equals(codeString))
				return VERY_SERIOUS_CONCERN;
			if ("extremely-serious-concern".equals(codeString))
				return EXTREMELY_SERIOUS_CONCERN;
			if ("present".equals(codeString))
				return PRESENT;
			if ("absent".equals(codeString))
				return ABSENT;
			if ("no-change".equals(codeString))
				return NO_CHANGE;
			if ("downcode1".equals(codeString))
				return DOWNCODE1;
			if ("downcode2".equals(codeString))
				return DOWNCODE2;
			if ("downcode3".equals(codeString))
				return DOWNCODE3;
			if ("upcode1".equals(codeString))
				return UPCODE1;
			if ("upcode2".equals(codeString))
				return UPCODE2;
			if (Configuration.isAcceptInvalidEnums())
				return null;
			else
				throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
		}

		public String toCode() {
			switch (this) {
				case HIGH:
					return "high";
				case MODERATE:
					return "moderate";
				case LOW:
					return "low";
				case VERY_LOW:
					return "very-low";
				case NO_CONCERN:
					return "no-concern";
				case SERIOUS_CONCERN:
					return "serious-concern";
				case VERY_SERIOUS_CONCERN:
					return "very-serious-concern";
				case EXTREMELY_SERIOUS_CONCERN:
					return "extremely-serious-concern";
				case PRESENT:
					return "present";
				case ABSENT:
					return "absent";
				case NO_CHANGE:
					return "no-change";
				case DOWNCODE1:
					return "downcode1";
				case DOWNCODE2:
					return "downcode2";
				case DOWNCODE3:
					return "downcode3";
				case UPCODE1:
					return "upcode1";
				case UPCODE2:
					return "upcode2";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getSystem() {
			switch (this) {
				case HIGH:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case MODERATE:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case LOW:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case VERY_LOW:
						return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case NO_CONCERN:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case SERIOUS_CONCERN:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case VERY_SERIOUS_CONCERN:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case EXTREMELY_SERIOUS_CONCERN:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case PRESENT:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case ABSENT:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case NO_CHANGE:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case DOWNCODE1:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case DOWNCODE2:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case DOWNCODE3:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case UPCODE1:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case UPCODE2:
					return "http://terminology.hl7.org/CodeSystem/certainty-rating";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDefinition() {
			switch (this) {
				case HIGH:
					return "High quality evidence.";
				case MODERATE:
					return "Moderate quality evidence.";
				case LOW:
					return "Low quality evidence.";
				case VERY_LOW:
					return "Very low quality evidence.";
				case NO_CONCERN:
					return "No serious concern.";
				case SERIOUS_CONCERN:
					return "Serious concern.";
				case VERY_SERIOUS_CONCERN:
					return "Very serious concern.";
				case EXTREMELY_SERIOUS_CONCERN:
					return "Extremely serious concern.";
				case PRESENT:
					return "Possible reason for increasing quality rating was checked and found to be present.";
				case ABSENT:
					return "Possible reason for increasing quality rating was checked and found to be absent.";
				case NO_CHANGE:
					return "No change to quality rating.";
				case DOWNCODE1:
					return "Reduce quality rating by 1.";
				case DOWNCODE2:
					return "Reduce quality rating by 2.";
				case DOWNCODE3:
					return "Reduce quality rating by 3.";
				case UPCODE1:
					return "Increase quality rating by 1.";
				case UPCODE2:
					return "Increase quality rating by 2.";
				case NULL:
					return null;
				default:
					return "?";
			}
		}

		public String getDisplay() {
			switch (this) {
				case HIGH:
					return "High quality";
				case MODERATE:
					return "Moderate quality";
				case LOW:
					return "Low quality";
				case VERY_LOW:
					return "Very low quality";
				case NO_CONCERN:
					return "No serious concern";
				case SERIOUS_CONCERN:
					return "Serious concern";
				case VERY_SERIOUS_CONCERN:
					return "Very serious concern";
				case EXTREMELY_SERIOUS_CONCERN:
					return "Extremely serious concern";
				case PRESENT:
					return "Present";
				case ABSENT:
					return "Absent";
				case NO_CHANGE:
					return "No change to rating";
				case DOWNCODE1:
					return "Reduce rating: -1";
				case DOWNCODE2:
					return "Reduce rating: -2";
				case DOWNCODE3:
					return "Reduce rating: -3";
				case UPCODE1:
					return "Increase rating: +1";
				case UPCODE2:
					return "Increase rating: +2";
				case NULL:
					return null;
				default:
					return "?";
			}
		}
	}

	public static class ArtifactAssessmentContentClassifierEnumFactory implements EnumFactory<ArtifactAssessmentContentClassifier> {
		public ArtifactAssessmentContentClassifier fromCode(String codeString) throws IllegalArgumentException {
			if (codeString == null || "".equals(codeString))
				if (codeString == null || "".equals(codeString))
					return null;
			if ("high".equals(codeString))
				return ArtifactAssessmentContentClassifier.HIGH;
			if ("moderate".equals(codeString))
				return ArtifactAssessmentContentClassifier.MODERATE;
			if ("low".equals(codeString))
				return ArtifactAssessmentContentClassifier.LOW;
			if ("very-low".equals(codeString))
				return ArtifactAssessmentContentClassifier.VERY_LOW;
			if ("no-concern".equals(codeString))
				return ArtifactAssessmentContentClassifier.NO_CONCERN;
			if ("serious-concern".equals(codeString))
				return ArtifactAssessmentContentClassifier.SERIOUS_CONCERN;
			if ("very-serious-concern".equals(codeString))
				return ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN;
			if ("extremely-serious-concern".equals(codeString))
				return ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN;
			if ("present".equals(codeString))
				return ArtifactAssessmentContentClassifier.PRESENT;
			if ("absent".equals(codeString))
				return ArtifactAssessmentContentClassifier.ABSENT;
			if ("no-change".equals(codeString))
				return ArtifactAssessmentContentClassifier.NO_CHANGE;
			if ("downcode1".equals(codeString))
				return ArtifactAssessmentContentClassifier.DOWNCODE1;
			if ("downcode2".equals(codeString))
				return ArtifactAssessmentContentClassifier.DOWNCODE2;
			if ("downcode3".equals(codeString))
				return ArtifactAssessmentContentClassifier.DOWNCODE3;
			if ("upcode1".equals(codeString))
				return ArtifactAssessmentContentClassifier.UPCODE1;
			if ("upcode2".equals(codeString))
				return ArtifactAssessmentContentClassifier.UPCODE2;
			throw new IllegalArgumentException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public Enumeration<ArtifactAssessmentContentClassifier> fromType(Base code) throws FHIRException {
			if (code == null)
				return null;
			if (code.isEmpty())
				return new Enumeration<ArtifactAssessmentContentClassifier>(this);
			String codeString = ((PrimitiveType) code).asStringValue();
			if (codeString == null || "".equals(codeString))
				return null;
			if ("high".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.HIGH);
			if ("moderate".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.MODERATE);
			if ("low".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.LOW);
			if ("very-low".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.VERY_LOW);
			if ("no-concern".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.NO_CONCERN);
			if ("serious-concern".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.SERIOUS_CONCERN);
			if ("very-serious-concern".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN);
			if ("extremely-serious-concern".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN);
			if ("present".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.PRESENT);
			if ("absent".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.ABSENT);
			if ("no-change".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.NO_CHANGE);
			if ("downcode1".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.DOWNCODE1);
			if ("downcode2".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.DOWNCODE2);
			if ("downcode3".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.DOWNCODE3);
			if ("upcode1".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.UPCODE1);
			if ("upcode2".equals(codeString))
				return new Enumeration<ArtifactAssessmentContentClassifier>(this, ArtifactAssessmentContentClassifier.UPCODE2);
			throw new FHIRException("Unknown ArtifactCommentType code '" + codeString + "'");
		}

		public String toCode(ArtifactAssessmentContentClassifier code) {
			if (code == ArtifactAssessmentContentClassifier.HIGH)
				return "high";
			if (code == ArtifactAssessmentContentClassifier.MODERATE)
				return "moderate";
			if (code == ArtifactAssessmentContentClassifier.LOW)
				return "low";
			if (code == ArtifactAssessmentContentClassifier.VERY_LOW)
				return "very-low";
			if (code == ArtifactAssessmentContentClassifier.NO_CONCERN)
				return "no-concern";
			if (code == ArtifactAssessmentContentClassifier.SERIOUS_CONCERN)
				return "serious-concern";
			if (code == ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN)
				return "very-serious-concern";
			if (code == ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN)
				return "extremely-serious-concern";
			if (code == ArtifactAssessmentContentClassifier.PRESENT)
				return "present";
			if (code == ArtifactAssessmentContentClassifier.ABSENT)
				return "absent";
			if (code == ArtifactAssessmentContentClassifier.NO_CHANGE)
				return "no-change";
			if (code == ArtifactAssessmentContentClassifier.DOWNCODE1)
				return "downcode1";
			if (code == ArtifactAssessmentContentClassifier.DOWNCODE2)
				return "downcode2";
			if (code == ArtifactAssessmentContentClassifier.DOWNCODE3)
				return "downcode3";
			if (code == ArtifactAssessmentContentClassifier.UPCODE1)
				return "upcode1";
			if (code == ArtifactAssessmentContentClassifier.UPCODE2)
				return "upcode2";
			return "?";
		}

		public String toSystem(ArtifactAssessmentContentClassifier code) {
			return code.getSystem();
		}
	}
	
	public enum ArtifactAssessmentContentType {}

	public enum ArtifactAssessmentWorkflowStatus {}

	public enum ArtifactAssessmentDisposition {}
	
	public static final String ARTIFACT_COMMENT_EXTENSION_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-artifactComment";
	public static final String CONTENT = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentContent";
	public static final String ARTIFACT = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentArtifact";
	public static final String CITEAS = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCiteAs";
	public static final String TITLE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentTitle";
	public static final String DATE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentDate";
	public static final String COPYRIGHT = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCopyright";
	public static final String APPROVAL_DATE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentApprovalDate";
	public static final String LAST_REVIEW_DATE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentLastReviewDate";
	public static final String WORKFLOW_STATUS = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentWorkflowStatus";

	public ArtifactAssessment() {
		super();
	}
	public ArtifactAssessment createArtifactComment(ArtifactAssessmentContentInformationType type, MarkdownType text, CanonicalType target, CanonicalType reference, Reference user)  throws FHIRException {
		setInfoTypeExtension(type);
		setSummaryExtension(text);
		setArtifactExtension(target);
		setRelatedArtifact(reference);
		setAuthorExtension(user);
		return this;
	}
	public boolean isValidArtifactComment(){
		return true;
	}
	public ArtifactAssessment setInfoTypeExtension(ArtifactAssessmentContentInformationType type) {
		if (type != null) {
			ArtifactAssessmentContentExtension ext;
			int contentIndex = findIndex(CONTENT, this.getExtension());
			if(contentIndex != -1){
				ext = (ArtifactAssessmentContentExtension) this.getExtension().get(contentIndex);
			} else {
				this.addExtension(new ArtifactAssessmentContentExtension());
				return this.setInfoTypeExtension(type);
			}
			ext.setInfoType(type);
		}
		return this;
	}

	public ArtifactAssessment setSummaryExtension(MarkdownType text) {
		if (text != null) {
			ArtifactAssessmentContentExtension ext;
			int contentIndex = findIndex(CONTENT, this.getExtension());
			if(contentIndex != -1){
				ext = (ArtifactAssessmentContentExtension) this.getExtension().get(contentIndex);
			} else {
				this.addExtension(new ArtifactAssessmentContentExtension());
				return this.setSummaryExtension(text);
			}
			ext.setSummary(text);
		}
		return this;
	}

	public ArtifactAssessment setArtifactExtension(CanonicalType target) {
		if (target != null) {
			int index = findIndex(ARTIFACT, this.getExtension());
			if(index != -1){
				this.extension.set(index, new ArtifactAssessmentArtifactExtension(target));
			} else {
				this.addExtension(new ArtifactAssessmentArtifactExtension(target));
			}
		}
		return this;
	}

	public ArtifactAssessment setRelatedArtifact(CanonicalType reference) {
		if (reference != null) {
			ArtifactAssessmentContentExtension ext;
			int contentIndex = findIndex(CONTENT, this.getExtension());
			if(contentIndex != -1){
				ext = (ArtifactAssessmentContentExtension) this.getExtension().get(contentIndex);
			} else {
				this.addExtension(new ArtifactAssessmentContentExtension());
				return this.setRelatedArtifact(reference);
			}
			ext.setRelatedArtifact(reference);
		}
		return this;
	}
	public ArtifactAssessment setAuthorExtension(Reference reference) {
		if (reference != null) {
			ArtifactAssessmentContentExtension ext;
			int contentIndex = findIndex(CONTENT, this.getExtension());
			if(contentIndex != -1){
				ext = (ArtifactAssessmentContentExtension) this.getExtension().get(contentIndex);
			} else {
				this.addExtension(new ArtifactAssessmentContentExtension());
				return this.setAuthorExtension(reference);
			}
			ext.setAuthorExtension(reference);
		}
		return this;
	}


	private int findIndex(String url, List<Extension> extensions){
		Optional<Extension> existingExtension =  extensions.stream()
			.filter(e -> e.getUrl().equals(url)).findAny();
			if(existingExtension.isPresent()){
				return extensions.indexOf(existingExtension.get());
			} else {
				return -1;
			}
	}
	@DatatypeDef(name="ArtifactAssessmentContentExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentContentExtension extends Extension {
		public static final String INFOTYPE = "informationType";
		public static final String SUMMARY = "summary";
		public static final String TYPE = "type";
		public static final String CLASSIFIER = "classifier";
		public static final String QUANTITY = "quantity";
		public static final String AUTHOR = "author";
		public static final String PATH = "path";
		public static final String RELATEDARTIFACT = "relatedArtifact";
		public static final String FREETOSHARE = "freeToShare";

		public ArtifactAssessmentContentExtension() throws FHIRException {
			super(CONTENT);
		}
		ArtifactAssessmentContentExtension setInfoType(ArtifactAssessmentContentInformationType infoType) throws FHIRException {
			if (infoType != null) {
				int index = findIndex(INFOTYPE, this.getExtension());
				if(index != -1){
					this.extension.set(index, new ArtifactAssessmentContentInformationTypeExtension(infoType));
				} else {
					this.addExtension(new ArtifactAssessmentContentInformationTypeExtension(infoType));
				}
			}
			return this;
		}
		ArtifactAssessmentContentExtension setSummary(MarkdownType summary) {
			if (summary != null) {
				int index = findIndex(SUMMARY, this.getExtension());
				if(index != -1){
					this.extension.set(index, new ArtifactAssessmentContentSummaryExtension(summary));
				} else {
					this.addExtension(new ArtifactAssessmentContentSummaryExtension(summary));
				}
			}
			return this;
		}
		ArtifactAssessmentContentExtension setRelatedArtifact(CanonicalType reference){
			if (reference != null) {
				int index = findIndex(RELATEDARTIFACT, this.getExtension());
				RelatedArtifact newRelatedArtifact = new RelatedArtifact();
				newRelatedArtifact.setType(RelatedArtifactType.CITATION);
				newRelatedArtifact.setResourceElement(reference);
				if(index != -1){
					this.extension.set(index, new ArtifactAssessmentContentRelatedArtifactExtension(newRelatedArtifact));
				} else {
					this.addExtension(new ArtifactAssessmentContentRelatedArtifactExtension(newRelatedArtifact));
				}
			}
			return this;
		}
		ArtifactAssessmentContentExtension setAuthorExtension(Reference author){
			if (author != null) {
				int index = findIndex(AUTHOR, this.getExtension());
				if(index != -1){
					this.extension.set(index, new ArtifactAssessmentContentAuthorExtension(author));
				} else {
					this.addExtension(new ArtifactAssessmentContentAuthorExtension(author));
				}
			}
			return this;
		}
		@DatatypeDef(name="ArtifactAssessmentContentInformationTypeExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentInformationTypeExtension extends Extension {
				public ArtifactAssessmentContentInformationTypeExtension(ArtifactAssessmentContentInformationType informationTypeCode) {
					super(INFOTYPE);
					Enumeration<ArtifactAssessmentContentInformationType> informationType = new Enumeration<ArtifactAssessmentContentInformationType>(new ArtifactAssessmentContentInformationTypeEnumFactory());
					informationType.setValue(informationTypeCode);
					this.setValue(informationType);
				}
		}

		@DatatypeDef(name="ArtifactAssessmentContentSummaryExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentSummaryExtension extends Extension {
			public ArtifactAssessmentContentSummaryExtension(MarkdownType summary) {
				super(SUMMARY, summary);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentTypeExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentTypeExtension extends Extension {
			public ArtifactAssessmentContentTypeExtension(Enumeration<ArtifactAssessmentContentType> contentType) {
				super(TYPE);
				CodeableConcept typeConcept = new CodeableConcept();
				typeConcept.addCoding(new Coding(
					contentType.getSystem(),
					contentType.getCode(),
					contentType.getDisplay()
				));
				this.setValue(typeConcept);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentClassifierExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentClassifierExtension extends Extension {
			public ArtifactAssessmentContentClassifierExtension(Enumeration<ArtifactAssessmentContentClassifier> classifier) {
				super(CLASSIFIER);
				CodeableConcept typeConcept = new CodeableConcept();
				typeConcept.addCoding(new Coding(
					classifier.getSystem(),
					classifier.getCode(),
					classifier.getDisplay()
				));
				this.setValue(typeConcept);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentQuantityExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentQuantityExtension extends Extension {
			public ArtifactAssessmentContentQuantityExtension(Quantity quantity) {
				super(QUANTITY, quantity);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentAuthorExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentAuthorExtension extends Extension {
			public ArtifactAssessmentContentAuthorExtension(Reference author) {
				super(AUTHOR, author);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentPathExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentPathExtension extends Extension {
			public ArtifactAssessmentContentPathExtension(UriType path) {
				super(PATH, path);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentRelatedArtifactExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentRelatedArtifactExtension extends Extension {
			public ArtifactAssessmentContentRelatedArtifactExtension(RelatedArtifact relatedArtifact) {
				super(RELATEDARTIFACT,relatedArtifact);
			}
		}

		@DatatypeDef(name="ArtifactAssessmentContentFreeToShareExtension", isSpecialization = true, profileOf = Extension.class)
		private class ArtifactAssessmentContentFreeToShareExtension extends Extension {
			public ArtifactAssessmentContentFreeToShareExtension(BooleanType freeToShare) {
				super(FREETOSHARE,freeToShare);
			}
		}
	}

	@DatatypeDef(name="ArtifactAssessmentArtifactExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentArtifactExtension extends Extension {
		public ArtifactAssessmentArtifactExtension(CanonicalType target) {
			super(ARTIFACT,target);
		}
		public ArtifactAssessmentArtifactExtension(Reference target) {
			super(ARTIFACT,target);
		}
		public ArtifactAssessmentArtifactExtension(UriType target) {
			super(ARTIFACT,target);
		}
	}

	@DatatypeDef(name="ArtifactAssessmentWorkflowStatusExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentWorkflowStatusExtension extends Extension {
		public ArtifactAssessmentWorkflowStatusExtension(Enumeration<ArtifactAssessmentWorkflowStatus> status) {
			super(WORKFLOW_STATUS,status);
		}
	}

	@DatatypeDef(name="ArtifactAssessmentDispositionExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentDispositionExtension extends Extension {
		public ArtifactAssessmentDispositionExtension(Enumeration<ArtifactAssessmentDisposition> disposition) {
			super(WORKFLOW_STATUS,disposition);
		}
	}

	@DatatypeDef(name="ArtifactAssessmentLastReviewDateExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentLastReviewDateExtension extends Extension {
		public ArtifactAssessmentLastReviewDateExtension(DateType lastReviewDate) {
			super(LAST_REVIEW_DATE,lastReviewDate);
		}

	}
	@DatatypeDef(name="ArtifactAssessmentApprovalDateExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentApprovalDateExtension extends Extension {
		public ArtifactAssessmentApprovalDateExtension(DateType approvalDate) {
			super(APPROVAL_DATE,approvalDate);
		}
	}
	@DatatypeDef(name="ArtifactAssessmentCopyrightExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentCopyrightExtension extends Extension {
		public ArtifactAssessmentCopyrightExtension(MarkdownType copyright) {
			super(COPYRIGHT,copyright);
		}
	}
	
	@DatatypeDef(name="ArtifactAssessmentDateExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentDateExtension extends Extension {
		public ArtifactAssessmentDateExtension(DateTimeType date) {
			super(DATE,date);
		}
	}
	
	@DatatypeDef(name="ArtifactAssessmentTitleExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentTitleExtension extends Extension {
		public ArtifactAssessmentTitleExtension(StringType title) {
			super(TITLE,title);
		}
	}
	
	@DatatypeDef(name="ArtifactAssessmentCiteAsExtension", isSpecialization = true, profileOf = Extension.class)
	private class ArtifactAssessmentCiteAsExtension extends Extension {
		public ArtifactAssessmentCiteAsExtension(Reference citation) {
			super(CITEAS,citation);
		}
		public ArtifactAssessmentCiteAsExtension(MarkdownType citation) {
			super(CITEAS,citation);
		}
	}
}