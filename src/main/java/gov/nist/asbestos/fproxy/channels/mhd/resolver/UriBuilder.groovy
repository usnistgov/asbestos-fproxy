package gov.nist.asbestos.fproxy.channels.mhd.resolver

import groovy.transform.TypeChecked

/**
 * this is necessary because URI(String) will not do encoding.  You need
 * the multi-parameter version to get that support
 */
@TypeChecked
class UriBuilder {

    static URI build(String ref) {
        try {
            if (ref.startsWith('#'))
                return new URI(null, '', ref)
            String[] parts = ref.split(':', 2)
            if (parts.size() == 1)
                return new URI(null, parts[0], null)
            String[] partx = parts[1].split('#')
            String scheme = parts[0]
            String ssp = partx[0]
            String[] party = ssp.split('#', 2)
            ssp = party[0]
            String fragment = null
            if (party.size() == 2)
                fragment = party[1]
            new URI(parts[0], parts[1], fragment)
        } catch (ArrayIndexOutOfBoundsException e) {
            throw e
        }
    }

    static String getId(URI uri) {
        String path = uri.path
        String[] parts = path.split('/')
        for (int i=0; i<parts.size()-1; i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i+1]
        }
        null
    }

    static String getResourceType(URI uri) {
        String path = uri.path
        String[] parts = path.split('/')
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i]
        }
        null
    }

    static URI getRelative(URI uri) {  // without base
        String path = uri.path
        List<String> parts = path.split('/') as List<String>
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return new URI(parts.subList(i, parts.size()).join('/'))
        }
        uri
    }

    static URI getBase(URI fullUrl) {
        String path = fullUrl.toString()
        List<String> parts = path.split('/') as List<String>
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts[i]))
                return new URI(parts.subList(0, i).join('/'))
        }
        fullUrl
    }

    static URI withNewId(URI fullUrl, String newId) {
        URI base = getBase(fullUrl)
        String type = getResourceType(fullUrl)
        new URI("${base}/${type}/${newId}")
    }

    static URI rebase(URI theURI, URI newBase) {
        new URI("${getBase(newBase)}/${getRelative(theURI)}")
    }

    static List<String> resourceNames = [
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
