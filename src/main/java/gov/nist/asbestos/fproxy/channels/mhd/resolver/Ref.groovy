package gov.nist.asbestos.fproxy.channels.mhd.resolver

import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import groovy.transform.TypeChecked
import org.hl7.fhir.instance.model.api.IBaseResource

@TypeChecked
class Ref {
    URI uri
    IBaseResource resource = null

    Ref(URI uri) {
        assert uri
        this.uri = uri
    }

    Ref(String ref) {
        assert ref != null  // may be ''
        uri = build(ref)
    }

    Ref(Ref ref) {
        this.uri = ref.uri
    }

    Ref(org.hl7.fhir.dstu3.model.Reference reference) {
        this(reference.reference)
    }

    // TODO implement
    boolean isLoadable() {
        false
    }

    // TODO implement
    ResourceWrapper load() {
        if (!isLoadable()) return null
        assert false
    }

    String getId() {
        String path = uri.path
        if (!path.contains('/')) return path
        String[] parts = path.split('/')
        for (int i=0; i<parts.size()-1; i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i+1]
        }
        null
    }

    String getResourceType() {
        String path = uri.path
        String[] parts = path.split('/')
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i]
        }
        null
    }

    Ref getRelative() {  // without base
        String path = uri.path
        List<String> parts = path.split('/') as List<String>
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return new Ref(parts.subList(i, parts.size()).join('/'))
        }
        new Ref('')
    }

    Ref getBase() {
        String path = uri.toString()
        List<String> parts = path.split('/') as List<String>
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return new Ref(parts.subList(0, i).join('/'))
        }
        new Ref(uri.toString())
    }

    Ref withNewId(String newId) {
        assert newId
        new Ref("${base}/${resourceType}/${newId}")
    }

    Ref rebase(String newBase) {
        assert newBase
        String theBase = new Ref(newBase).getBase()
        new Ref("${theBase}/${relative}")
    }

    // TODO needs test
    Ref rebase(Ref newBase) {
        assert newBase
        new Ref("${newBase.base}/${relative}")
    }

    Ref getFull() {  // without version
        String baseStr = base as String
        String resourceTypeStr = resourceType as String
        String idStr = id as String
        boolean hasScheme = uri.scheme
        if (!hasScheme && baseStr && resourceTypeStr && idStr)
            return new Ref("${base}/${resourceType}/${id}")
        if (!hasScheme && resourceTypeStr && !idStr)
            return new Ref("${resourceType}")
        if (!hasScheme && resourceTypeStr && idStr)
            return new Ref("${resourceType}/${id}")
        if (uri.toString().contains('_history'))
            return new Ref(uri.toString().split('/_history', 2)[0])
        return new Ref(uri)
    }

    String getVersion() {
        String[] parts = uri.toString().split('_history/')
        if (parts.size() < 2)
            return null
        parts[1]
    }

    boolean isAbsolute() {
        uri.scheme && base && resourceType && id
    }

    String toString() {
        uri.toString()
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Ref ref = (Ref) o

        if (uri != ref.uri) return false

        return true
    }

    int hashCode() {
        return (uri != null ? uri.hashCode() : 0)
    }

    boolean contained = false

    private URI build(String ref) {
        if (ref.startsWith('#')) {
            contained = true
            ref = ref.substring(1)
        }
        new URI(ref)
//        try {
//            if (ref.startsWith('#'))
//                return new URI(null, '', ref)
//            String[] parts = ref.split(':', 2)
//            if (parts.size() == 1)
//                return new URI(null, parts[0], null)
//            String[] partx = parts[1].split('#')
//            String scheme = parts[0]
//            String ssp = partx[0]
//            String[] party = ssp.split('#', 2)
//            ssp = party[0]
//            String fragment = null
//            if (party.size() == 2)
//                fragment = party[1]
//            new URI(parts[0], parts[1], fragment)
//        } catch (ArrayIndexOutOfBoundsException e) {
//            throw e
//        }
    }

    static private List<String> resourceNames = [
            'CapabilityStatement',
            'StructureDefinition',
            'ImplementationGuide',
            'SearchParameter',
            'MessageDefinition',
            'OperationDefinition',
            'CompartmentDefinition',
            'StructureMap',
            'GraphDefinition',
            'ExampleScenario',
            'CodeSystem',
            'ValueSet',
            'ConceptMap',
            'NamingSystem',
            'TerminologyCapabilities',
            'Provenance',
            'AuditEvent',
            'Consent',
            'Composition',
            'DocumentManifest',
            'DocumentReference',
            'CatalogEntry',
            'Basic',
            'Binary',
            'Bundle',
            'Linkage',
            'MessageHeader',
            'OperationOutcome',
            'Parameters',
            'Subscription',
            'Patient',
            'Practitioner',
            'PractitionerRole',
            'RelatedPerson',
            'Person',
            'Group',
            'Organization',
            'OrganizationAffiliation',
            'HealthcareService',
            'Endpoint',
            'Location',
            'Substance',
            'BiologicallyDerivedProduct',
            'Device',
            'DeviceMetric',
            'Task',
            'Appointment',
            'AppointmentResponse',
            'Schedule',
            'Slot',
            'VerificationResult',
            'Encounter',
            'EpisodeOfCare',
            'Flag',
            'List',
            'Library',
            'AllergyIntolerance',
            'AdverseEvent',
            'Condition',
            'Procedure',
            'FamilyMemberHistory',
            'ClinicalImpression',
            'DetectedIssue',
            'Observation',
            'Media',
            'DiagnosticReport',
            'Specimen',
            'BodyStructure',
            'ImagingStudy',
            'QuestionnaireResponse',
            'MolecularSequence',
            'MedicationRequest',
            'MedicationAdministration',
            'MedicationDispense',
            'MedicationStatement',
            'Medication',
            'MedicationKnowledge',
            'Immunization',
            'ImmunizationEvaluation',
            'ImmunizationRecommendation',
            'CarePlan',
            'CareTeam',
            'Goal',
            'ServiceRequest',
            'NutritionOrder',
            'VisionPrescription',
            'RiskAssessment',
            'RequestGroup',
            'Communication',
            'CommunicationRequest',
            'DeviceRequest',
            'DeviceUseStatement',
            'GuidanceResponse',
            'SupplyRequest',
            'SupplyDelivery',
            'Coverage',
            'CoverageEigibilityRequest',
            'CoverageEligibilityResponse',
            'EnrollmentRequest',
            'EnrollmentResponse',
            'Claim',
            'ClaimResponse',
            'Invoice',
            'PaymentNotice',
            'PaymentReconciliation',
            'Account',
            'ChargeItem',
            'ChargeItemDefinition',
            'Contract',
            'ExplanationOfBenefit',
            'InsurancePlan',
            'ResearchStudy',
            'ResearchSubject',
            'ActivityDefinition',
            'DeviceDefinition',
            'EventDefinition',
            'ObservationDefinition',
            'PlanDefinition',
            'Questionnaire',
            'SpecimenDefinition',
            'ResearchDefinition',
            'ResearchElementDefinition',
            'Evidence',
            'EvidenceVariable',
            'EffectEvidenceSynthesis',
            'RiskEvidenceSynthesis',
            'Measure',
            'MeasureReport',
            'TestScript',
            'TestReport',
            'MedicinalProduct',
            'MedicinalProductAuthorization',
            'MedicinalProductContraindication',
            'MedicinalProductIndication',
            'MedicinalProductIngredient',
            'MedicinalProductInteraction',
            'MedicinalProductManufactured',
            'MedicinalProductPackaged',
            'MedicinalProductPharmaceutical',
            'MedicinalProductUndesirableEffect 0',
            'SubstancePolymer',
            'SubstanceReferenceInformation',
            'SubstanceSpecification'
    ]


}
