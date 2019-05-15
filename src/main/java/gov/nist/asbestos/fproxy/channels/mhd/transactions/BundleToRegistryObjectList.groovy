package gov.nist.asbestos.fproxy.channels.mhd.transactions

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceCacheMgr
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceMgr
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Configuration
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.MhdIdentifier
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.Submission
import gov.nist.asbestos.simapi.tk.util.UuidAllocator
import gov.nist.asbestos.simapi.validation.Val
import groovy.transform.TypeChecked
import groovy.xml.MarkupBuilder
import org.apache.log4j.Logger
import org.hl7.fhir.dstu3.model.Binary
import org.hl7.fhir.dstu3.model.Bundle
import org.hl7.fhir.dstu3.model.DocumentManifest
import org.hl7.fhir.dstu3.model.DocumentReference
import org.hl7.fhir.dstu3.model.Identifier
import org.hl7.fhir.dstu3.model.ListResource
import org.hl7.fhir.instance.model.api.IBaseResource

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


    BundleToRegistryObjectList(ResourceCacheMgr resourceCacheMgr, Val val) {
        this.resourceCacheMgr = resourceCacheMgr
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

        rMgr.resources.each { Ref uri, IBaseResource resource ->
            if (!acceptableResourceTypes.contains(resource.class)) {
                val.warn(new Val()
                        .msg("Resource type ${resource.class.simpleName} is not part of MHD and will be ignored"))
                        .frameworkDoc(mhdProfileRef)
            }
        }
    }

    Submission buildSubmission(Bundle bundle, Configuration config) {
        ResourceMgr rMgr = new ResourceMgr(bundle, val).addResourceCacheMgr(resourceCacheMgr)
        scanBundleForAcceptability(bundle, rMgr)

        Submission submission = buildRegistryObjectList(rMgr, config)

        def writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)
        submission.documentIdToContentId.each { id, contentId ->
            addDocument(builder, id, contentId, val)
        }
        submission.documentDefinitions = writer.toString()

        submission
    }

    Submission buildRegistryObjectList(ResourceMgr rMgr, Configuration config) {
        Map<IBaseResource, String> allocatedIds = [:]
        Submission submission = new Submission()
        submission.contentId = 'm' + baseContentId

        int index = 1

        // assign entryUUIDs to all DocumentManifests and DocumentReferences
        // if they already have one, leave it
        // othewise assign symbolic id
        rMgr.resources.findAll { Ref uri, IBaseResource resource ->
            resource instanceof DocumentReference || resource instanceof DocumentManifest
        }.each { Ref uri, IBaseResource dr ->
            String id = allocationSymbolicId()
            val.msg("Assigning ${id} to ${dr.class.simpleName}/${dr.idElement.value}")
            allocatedIds[dr] = id
        }

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.RegistryObjectList(xmlns: 'urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0') {
            rMgr.resources.each { Ref url, IBaseResource resource ->
                if (resource instanceof DocumentManifest) {
                    rMgr.assignId(resource)
                    er.sectionHeading("DocumentManifest(${resource.id})  URL is ${url}")
                    proxyBase.resourcesSubmitted << resource
                    rMgr.currentResource(resource)
                    DocumentManifest dm = (DocumentManifest) resource
                    addSubmissionSet(xml, dm.getId(), dm)
                    addSubmissionSetAssociations(xml, dm)
                }
                else if (resource instanceof DocumentReference) {
                    rMgr.assignId(resource)
                    er.sectionHeading("DocumentReference(${resource.id})  URL is ${url}")
                    if (proxyBase)
                        proxyBase.resourcesSubmitted << resource
                    rMgr.currentResource(resource)
                    DocumentReference dr = (DocumentReference) resource
                    def (ref, binary) = rMgr.resolveReference(url, UriBuilder.build(dr.content[0].attachment.url), new ResolverConfig().internalRequired())
                    er.detail("References Binary ${ref}")
                    assert binary instanceof Binary, "Binary ${dr.content[0].attachment.url} is not available in Bundle."
                    Binary b = binary
                    b.id = dr.masterIdentifier.value
                    String proxyFhirBase = ''
                    if (proxyBase)
                        proxyFhirBase = proxyBase.config.getEndpoint(TransactionType.FHIR)
                    dr.content[0].attachment.url = proxyFhirBase + '/' + 'Binary/' + dr.masterIdentifier.value
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

    static List<MhdIdentifier> getIdentifiers(IBaseResource resource) {
        assert resource instanceof DocumentManifest || resource instanceof DocumentReference

        List<Identifier> identifiers = (resource instanceof DocumentManifest) ?
                ((DocumentManifest) resource).identifier :
                ((DocumentReference) resource).identifier
        identifiers.collect { Identifier ident -> new MhdIdentifier(ident)}
    }


}
