package gov.nist.asbestos.fproxy.channels.mhd.transactions

import gov.nist.asbestos.fproxy.Base.LoadedResource
import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResolverConfig
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceCacheMgr
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceMgr
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Attachment
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Code
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.CodeTranslator
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Configuration
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.MhdIdentifier
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Submission
import gov.nist.asbestos.simapi.tk.util.UuidAllocator
import gov.nist.asbestos.simapi.validation.Val
import groovy.transform.TypeChecked
import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger
import org.hl7.fhir.dstu3.model.Base64BinaryType
import org.hl7.fhir.dstu3.model.Binary
import org.hl7.fhir.dstu3.model.Bundle
import org.hl7.fhir.dstu3.model.CodeableConcept
import org.hl7.fhir.dstu3.model.Coding
import org.hl7.fhir.dstu3.model.DocumentManifest
import org.hl7.fhir.dstu3.model.DocumentReference
import org.hl7.fhir.dstu3.model.Identifier
import org.hl7.fhir.dstu3.model.ListResource
import org.hl7.fhir.dstu3.model.Patient
import org.hl7.fhir.dstu3.model.Reference
import org.hl7.fhir.instance.model.api.IBaseResource

import javax.xml.bind.DatatypeConverter

/**
 *
 */

// TODO - add legalAuthenticator
// TODO - add sourcePatientInfo
// TODO - add referenceIdList
// TODO - add author
// TODO - add case where Patient not in bundle?????

/**
 * Association id = ID06
 *    source = SubmissionSet_ID02
 *    target = Document_ID01
 *
 * RegistryPackage id = 234...
 *
 * ExtrinsicObject id = ID07
 */

@TypeChecked
class BundleToRegistryObjectList {
    static private final Logger logger = Logger.getLogger(BundleToRegistryObjectList.class)
    ResourceCacheMgr resourceCacheMgr
    static acceptableResourceTypes = [DocumentManifest, DocumentReference, Binary, ListResource]
    static String comprehensiveMetadataProfile = 'http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle'
    static String minimalMetadataProfile = 'http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle'
    static List<String> profiles = [comprehensiveMetadataProfile, minimalMetadataProfile]
    Val val
    String baseContentId = '.de1e4efca5ccc4886c8528535d2afb251e0d5fa31d58a815@ihexds.nist.gov'
    String bundleProfile
    String mhdProfileRef = 'MHD Profile - Rev 3.1'
    CodeTranslator codeTranslator
    ResourceMgr rMgr


    BundleToRegistryObjectList(ResourceCacheMgr resourceCacheMgr, CodeTranslator codeTranslator, Val val) {
        this.resourceCacheMgr = resourceCacheMgr
        this.codeTranslator = codeTranslator
        this.val = val
    }

    void scanBundleForAcceptability(Bundle bundle, ResourceMgr rMgr) {
        if (bundle.meta.profile.size() != 1)
            val.err(new Val()
                    .msg('No profile declaration present in bundle')
                    .frameworkDoc('3.65.4.1.2.1 Bundle Resources'))
        bundleProfile = bundle.meta.profile
        if (!profiles.contains(bundleProfile))
            val.err(new Val()
                    .msg("Do not understand profile declared in bundle - ${bundleProfile}")
                    .frameworkDoc('3.65.4.1.2.1 Bundle Resources'))

        rMgr.resources.each { Ref uri, ResourceWrapper resource ->
            if (!acceptableResourceTypes.contains(resource.resource.class)) {
                val.warn(new Val()
                        .msg("Resource type ${resource.resource.class.simpleName} is not part of MHD and will be ignored"))
                        .frameworkDoc(mhdProfileRef)
            }
        }
    }

    Submission buildSubmission(Bundle bundle, Configuration config) {
        rMgr = new ResourceMgr(bundle, val).addResourceCacheMgr(resourceCacheMgr)
        scanBundleForAcceptability(bundle, rMgr)

        Submission submission = buildRegistryObjectList(config)

        def writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)
        submission.documentIdToContentId.each { id, contentId ->
            addDocument(builder, id, contentId, val)
        }
        submission.documentDefinitions = writer.toString()

        submission
    }

    // TODO handle List/Folder or signal error
    private Submission buildRegistryObjectList(Configuration config) {
        Submission submission = new Submission()
        submission.contentId = 'm' + baseContentId

        int index = 1

        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.RegistryObjectList(xmlns: 'urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0') {
            rMgr.resources.each { Ref url, ResourceWrapper resource ->
                if (resource.resource instanceof DocumentManifest) {
                    DocumentManifest dm = (DocumentManifest) resource.resource
                    addSubmissionSet(xml, resource)
                    addSubmissionSetAssociations(xml, dm)
                }
                else if (resource instanceof DocumentReference) {
                    rMgr.assignId(resource)
                    DocumentReference dr = (DocumentReference) resource
                    LoadedResource loadedResource = rMgr.resolveReference(url, new Ref(dr.content[0].attachment.url), new ResolverConfig().internalRequired())
                    if (!(loadedResource.resource.resource instanceof Binary))
                        val.err(new Val()
                        .msg("Binary ${dr.content[0].attachment.url} is not available in Bundle."))
                    Binary b = (Binary) loadedResource.resource.resource
                    b.id = dr.masterIdentifier.value
//                    String proxyFhirBase = ''
//                    if (proxyBase)
//                        proxyFhirBase = proxyBase.config.getEndpoint(TransactionType.FHIR)
//                    dr.content[0].attachment.url = proxyFhirBase + '/' + 'Binary/' + dr.masterIdentifier.value
                    Attachment a = new Attachment()
                    a.contentId = Integer.toString(index) + baseContentId
                    a.contentType = b.contentType
                    a.content = b.content
                    submission.attachments << a
                    index++

                    addExtrinsicObject(xml, dr.getId(), dr)
//                    documents[dr.getId()] = a.contentId
                    documents[getEntryUUID(dr)] = a.contentId
                    addRelationshipAssociations(xml, rMgr.url(dr), dr)
                } else {
                    if (proxyBase)
                        proxyBase.resourcesSubmitted << resource

                }
            }
        }
        submission.registryObjectList = writer.toString()
        submission
    }

    private addSubmissionSetAssociations(MarkupBuilder xml, ResourceWrapper resource) {
        DocumentManifest dm = (DocumentManifest) resource.resource
        if (!dm.content) return
        dm.content.each { DocumentManifest.DocumentManifestContentComponent component ->
            Reference ref = component.PReference
            LoadedResource loadedResource = rMgr.resolveReference(null, new Ref(ref.reference), new ResolverConfig().internalRequired())

            if (!loadedResource.resource)
                val.err(new Val()
                .msg("DocumentManifest references ${ref.resource} - ${loadedResource.ref} is not included in the bundle"))
            addAssociation(xml, 'urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember', resource.assignedId, loadedResource.resource.assignedId, 'SubmissionSetStatus', ['Original'])
        }
    }

    private addAssociation(MarkupBuilder xml, String type, String source, String target, String slotName, List<String> slotValues) {
        val.add(new Val().msg("Association(${type}) source=${source} target=${target}"))
        def assoc = xml.Association(
                sourceObject: "${source}",
                targetObject: "${target}",
                associationType: "${type}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Association',
                id: "${rMgr.newId()}"
        ) {
            if (slotName) {
                addSlot(xml, slotName, slotValues)
            }
        }
        return assoc
    }

    private addSubmissionSet(MarkupBuilder builder, ResourceWrapper resource) {
        DocumentManifest dm = (DocumentManifest) resource.resource

        val.add(new Val().msg("SubmissionSet(${resource.assignedId})"))
        builder.RegistryPackage(
                id: resource.assignedId,
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage',
                status: 'urn:oasis:names:tc:ebxml-regrep:StatusType:Approved') {

            if (dm.created)
                addSlot(builder, 'submissionTime', [translateDateTime(dm.created)])

            if (dm.description)
                addName(builder, dm.description)

            addClassification(builder, 'urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd', rMgr.allocationSymbolicId(), resource.assignedId)

            if (dm.type)
                addClassificationFromCodeableConcept(builder, dm.type, 'urn:uuid:aa543740-bdda-424e-8c96-df4873be8500', resource.assignedId)

            if (!dm.masterIdentifier?.value)
                val.err(new Val()
                .msg('DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]')
                .frameworkDoc('Table 4.5.1.2-1: FHIR DocumentManifest mapping to SubmissionSet'))

            String masterId = (dm.masterIdentifier?.value) ? dm.masterIdentifier.value : null
            if (masterId)
                addExternalIdentifier(builder, 'urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8', unURN(masterId), rMgr.newId(), resource.assignedId, 'XDSSubmissionSet.uniqueId')

            if (dm.source) {
                addExternalIdentifier(builder, 'urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832', unURN(dm.source), rMgr.newId(), resource.assignedId, 'XDSSubmissionSet.sourceId')
            }

            if (dm.subject)
                addSubject(builder, resource.fullUrl, resource.assignedId, 'urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446', dm.subject, 'XDSSubmissionSet.patientId')

        }
    }

    /**
     * add ExtrinsicObject.
     * Official Identifier (entryUUID) must be set and will be used in translation.
     * @param builder
     * @param fullUrl
     * @param dr - DocumentReference to source from
     * @return
     */
    def addExtrinsicObject(MarkupBuilder builder, ResourceWrapper resource) {
        DocumentReference dr = (DocumentReference) resource.resource
        assert dr.content, 'DocumentReference has no content section'
        assert dr.content.size() == 1, 'DocumentReference has multiple content sections'
        assert dr.content[0].attachment, 'DocumentReference has no content/attachment'

        String entryUUID = getEntryUUID(dr)

        if (!entryUUID) {
            entryUUID = allocateSymbolicId()
            logger.info("Assigning ${entryUUID} to ${fullUrl} in addExtrinsicObject")
            setEntryUUID(dr, entryUUID)  // updating in-memory copy
        } else {
            // there was an entryUUID - verify it is of valid format unless it is
            // a symbolic ID we assigned
            String id = getEntryUUID(dr)
            if (!id.startsWith('SymbolicId'))
                checkEntryUUID(dr)
        }

        builder.ExtrinsicObject(
                id: entryUUID,
                objectType:'urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1',
                mimeType: dr.content[0].attachment.contentType)
                //status: getStatus(dr))
                {
                    // 20130701231133
                    if (dr.indexed)
                        addSlot(builder, 'creationTime', [translateDateTime(dr.indexed)])

                    if (dr.context?.period?.start)
                        addSlot(builder, 'serviceStartTime', [translateDateTime(dr.context.period.start)])

                    if (dr.context?.period?.end)
                        addSlot(builder, 'serviceStopTime', [translateDateTime(dr.context.period.end)])

                    if (dr.content[0].attachment.language)
                        addSlot(builder, 'languageCode', dr.content.attachment.language)

                    if (dr.content?.attachment?.url && translateForDisplay)
                        addSlot(builder, 'repositoryUniqueId', dr.content.attachment.url)

                    if (dr.content[0].attachment.hashElement.value) {
                        Base64BinaryType hash64 = dr.content[0].attachment.hashElement
                        logger.info("value is ${hash64.getValue()}")
                        logger.info("base64Binary is ${hash64.asStringValue()}")
                        byte[] hash = hash64.getValue() //DatatypeConverter.parseBase64Binary(hash64.asStringValue())
                        logger.info("via groovy = ${hash.encodeHex().toString()}")

                        logger.info("encoded is ${hash.toString()}")

                        String hashString = DatatypeConverter.printHexBinary(hash).toLowerCase()
                        logger.info("hexBinary is ${hashString}")
                        addSlot(builder, 'hash', [hashString])

//                        byte[] hash = HashTranslator.toByteArray(hash64.toString())
//                        byte[] hash = HashTranslator.toByteArrayFromBase64Binary(hash64.asStringValue())
//                        String hashString = hash.encodeHex().toString() as String
//                        addSlot(builder, 'hash', [hashString])
                    }

                    if (dr.context?.sourcePatientInfo)
                        this.addSourcePatient(builder, dr.context.sourcePatientInfo)

                    if (dr.description)
                        addName(builder, dr.description)

                    if (dr.type)
                        addClassificationFromCodeableConcept(builder, dr.type, 'urn:uuid:f0306f51-975f-434e-a61c-c59651d33983', entryUUID)

                    if (dr.class_)
                        addClassificationFromCodeableConcept(builder, dr.class_, 'urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a', entryUUID)

                    if (dr.securityLabel?.coding)
                        addClassificationFromCoding(builder, dr.securityLabel[0].coding[0], 'urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f', entryUUID)

                    if (dr.content.format.size() > 0) {
                        Coding format = dr.content.format[0]
                        if (format.system)
                            addClassificationFromCoding(builder, dr.content[0].format, 'urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d', entryUUID)
                    }

                    if (dr.context?.facilityType)
                        addClassificationFromCodeableConcept(builder, dr.context.facilityType, 'urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1', entryUUID)

                    if (dr.context?.practiceSetting)
                        addClassificationFromCodeableConcept(builder, dr.context.practiceSetting, 'urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead', entryUUID)

                    if (dr.context?.event)
                        addClassificationFromCodeableConcept(builder, dr.context.event?.first(), 'urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4', entryUUID)

                    assert dr.masterIdentifier, 'DocumentReference.masterIdentifier not present - declared by IHE to be [1..1]'
                    assert dr.masterIdentifier.value, 'DocumentReference.masterIdentifier has no value - declared by IHE to be [1..1]'
                    String masterId
                    if (dr.masterIdentifier?.value) {
                        masterId = unURN(dr.masterIdentifier.value)
//                    } else {
//                        masterId = UniqueIdAllocator.getInstance().allocate()
                    }
                    addExternalIdentifier(builder, 'urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab', masterId, rMgr.newId(), entryUUID, 'XDSDocumentEntry.uniqueId')

                    if (dr.subject?.hasReference())
                        addSubject(builder, fullUrl, entryUUID, 'urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427', dr.subject, 'XDSDocumentEntry.patientId')

                    if (dr.author) {
                        dr.author.each { Reference ref ->

                        }
                    }

                }
        return entryUUID
    }


    // TODO must be absolute reference
    // TODO official identifiers must be changed
    private addSubject(MarkupBuilder builder, Ref fullUrl, String containingObjectId, String scheme,  org.hl7.fhir.dstu3.model.Reference subject, String attName) {
        Ref ref1 = new Ref(subject.getReference())
        LoadedResource loadedResource = rMgr.resolveReference(fullUrl, ref1, new ResolverConfig().externalRequired())
        if (!loadedResource.ref) {
            val.err(new Val()
                    .msg("${fullUrl} makes reference to ${ref1}")
                    .msg('All DocumentReference.subject and DocumentManifest.subject values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).')
                    .frameworkDoc('3.65.4.1.2.2 Patient Identity'))
        }
        if (!(loadedResource.resource instanceof Patient))
            val.err(new Val()
                    .msg("${fullUrl} points to a ${loadedResource.resource.class.simpleName} - it must be a Patient")
                    .frameworkDoc('3.65.4.1.2.2 Patient Identity'))

        Patient patient = (Patient) loadedResource.resource

        List<Identifier> identifiers = patient.getIdentifier()
        Identifier official = getOfficial(identifiers)
        assert official, 'Patient has no official identifier'

        assert official.value, 'Patient resource has no value on its official identifier (${url})'
        assert official.system, 'Patient resource has no system on its official identifier (${url})'

        String value = official.value
        String system = official.system
        String oid = unURN(system)
        def pid = "${value}^^^&${oid}&ISO"

        addExternalIdentifier(builder, scheme, pid, rMgr.newId(), containingObjectId, attName)
    }

    private addExternalIdentifier(MarkupBuilder builder, String scheme, String value, String id, String registryObject, String name) {
        val.add(new Val().msg("ExternalIdentifier ${scheme}"))
        builder.ExternalIdentifier(
                identificationScheme: scheme,
                value: "${value}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier',
                registryObject: "${registryObject}") {
            Name() {
                LocalizedString(value: "${name}")
            }
        }
    }

    // TODO - no profile guidance on how to convert coding.system URL to existing OIDs

    private addClassificationFromCodeableConcept(MarkupBuilder builder, CodeableConcept cc, String scheme, String classifiedObjectId) {
        Coding coding = cc.coding[0]
        if (coding)
            addClassificationFromCoding(builder, coding, scheme, classifiedObjectId)
    }

    private addClassificationFromCoding(MarkupBuilder builder, Coding coding, String scheme, String classifiedObjectId) {
        Code systemCode = codeTranslator.findCodeByClassificationAndSystem(scheme, coding.system, coding.code)
        if (!systemCode, "Cannot find translation for code ${coding.system}|${coding.code} (FHIR) into XDS coding scheme ${scheme} in configured codes.xml file"
        addClassification(builder, scheme, rMgr.allocationSymbolicId(), classifiedObjectId, coding.code, systemCode.codingScheme, coding.display)
    }

    /**
     * add external classification (see ebRIM for definition)
     * @param builder
     * @param scheme
     * @param id
     * @param registryObject
     * @param value
     * @param codeScheme
     * @param displayName
     * @return
     */
    private addClassification(MarkupBuilder builder, String scheme, String id, String registryObject, String value, String codeScheme, String displayName) {
        builder.Classification(
                classificationScheme: "${scheme}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification',
                nodeRepresentation: "${value}",
                classifiedObject: "${registryObject}"
        ) {
            addSlot(builder, 'codingScheme', [codeScheme])
            addName(builder, displayName)
        }
    }

    private addClassification(MarkupBuilder builder, String node, String id, String classifiedObject) {
        val.add(new Val().msg("Classification ${node}"))
        builder.Classification(
                classifiedObject: "${classifiedObject}",
                classificationNode: "${node}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification')
    }

    private addSlot(MarkupBuilder builder, String name, List<String> values) {
        val.add(new Val().msg("Slot ${name}"))
        builder.Slot(name: name) {
            ValueList {
                values.each {
                    Value "${it}"
                }
            }
        }
    }

    private addName(MarkupBuilder builder, String value) {
        val.add(new Val().msg("Name"))
        builder.Name() {
            LocalizedString(value: "${value}")
        }
    }


    static List<MhdIdentifier> getIdentifiers(IBaseResource resource) {
        assert resource instanceof DocumentManifest || resource instanceof DocumentReference

        List<Identifier> identifiers = (resource instanceof DocumentManifest) ?
                ((DocumentManifest) resource).identifier :
                ((DocumentReference) resource).identifier
        identifiers.collect { Identifier ident -> new MhdIdentifier(ident)}
    }

    private static String unURN(String uuid) {
        if (uuid.startsWith('urn:uuid:')) return uuid.substring(9)
        if (uuid.startsWith('urn:oid:')) return uuid.substring(8)
        return uuid
    }

}
