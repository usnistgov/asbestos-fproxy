package gov.nist.asbestos.fproxy.channels.mhd.resolver

import gov.nist.asbestos.fproxy.Base.Base
import gov.nist.asbestos.fproxy.Base.FhirClient
import gov.nist.asbestos.fproxy.Base.LoadedResource
import gov.nist.asbestos.simapi.tk.util.UuidAllocator
import gov.nist.asbestos.simapi.validation.Val
import gov.nist.asbestos.simapi.validation.ValidationReport
import groovy.transform.TypeChecked
import org.apache.http.client.utils.URIBuilder
import org.apache.log4j.Logger
import org.hl7.fhir.dstu3.model.Bundle
import org.hl7.fhir.dstu3.model.DocumentManifest
import org.hl7.fhir.dstu3.model.DocumentReference
import org.hl7.fhir.dstu3.model.DomainResource
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.instance.model.api.IBaseResource


/**
 *
 */
@TypeChecked
class ResourceMgr {
    static private final Logger logger1 = Logger.getLogger(ResourceMgr.class);
    Bundle bundle = null
    // Object is some Resource type
    Map<URI, IBaseResource> resources = [:]   // url -> resource
    int newIdCounter = 1

    // for current resource
    Map<URI, IBaseResource> containedResources = [:]
    URI fullUrl

    // resource cache mgr
    ResourceCacheMgr resourceCacheMgr = null

    ValidationReport validationReport
    Val topVal

    ResourceMgr(Bundle bundle, ValidationReport validationReport) {
        this.bundle = bundle
        this.validationReport = validationReport
        topVal = new Val()
        validationReport.add(topVal)
        if (bundle)
            parseBundle()
    }

    ResourceMgr(Map<URI, Resource> resourceMap, ValidationReport validationReport) {
        this.validationReport = validationReport
        validationReport.add(topVal)
        parseResourceMap(resourceMap)
    }

    def addResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr
    }

    def parseResourceMap(Map<URI, Resource> resourceMap) {
        resourceMap.each { URI fullUrl, Resource res ->
            assignId(res)
            addResource(fullUrl, res)
        }
    }

    def parseBundle() {
        Val thisVal = topVal.addSection("Load Bundle...")
        bundle.getEntry().each { Bundle.BundleEntryComponent component ->
            if (component.hasResource()) {
                assignId(component.getResource())
                thisVal.add("...${component.fullUrl}")
                def duplicate = addResource(new URI(component.fullUrl), component.getResource())
                assert !duplicate, "Duplicate entry in bundle - URL is ${component.fullUrl}"
            }
        }
            Val r = thisVal.addSection('Load Resources')
            r.msg(toString())
    }

    def currentResource(Resource resource) {
        clearContainedResources()
        assert resource instanceof DomainResource
        def contained = resource.contained
        contained?.each { Resource r ->
            def duplicate = addContainedResource(r)
            assert !duplicate, "Duplicate contained Resource (${r.id} in Resource ${resource.id})"
        }

        fullUrl = url(resource)
    }



    String assignId(Resource resource) {
        if (!resource.id || Base.isUUID(resource.id)) {
            if (resource instanceof DocumentReference)
                resource.id = UuidAllocator.allocate()
            else if (resource instanceof DocumentManifest)
                resource.id = UuidAllocator.allocate()
            else
                resource.id = newId()
        }
        return resource.id
    }

    def newId() { String.format("ID%02d", newIdCounter++) }

    String toString() {
        StringBuilder buf = new StringBuilder()
        buf.append("Resources:\n")

        resources.each { url, resource ->
            buf.append(url).append('   ').append(resource.class.simpleName).append('\n')
        }
        buf
    }

    Object getResource(referenceUrl) {
        return resources[referenceUrl]
    }

    List getResourceObjects() {
        resources.values() as List
    }

    /**
     *
     * @param url
     * @param resource
     * @return already present
     */
    boolean addResource(URI url, IBaseResource resource) {
        boolean duplicate = resources.containsKey(url)
        resources[url] = resource
        return duplicate
    }

    def addContainedResource(IBaseResource resource) {
        assert resource instanceof DomainResource
        def id = UriBuilder.build(resource.id)
        boolean duplicate = containedResources.containsKey(id)
        containedResources[id] = resource
        return duplicate
    }

    def clearContainedResources() {
        containedResources = [:]
    }

    IBaseResource getContainedResource(URI id) {
        assert id
        return containedResources[id]
    }

    URI url(resource) {
        resources.entrySet().find { Map.Entry entry ->
            entry.value == resource
        }?.key
    }

    /**
     *
     * @param type
     * @return list of [URI, Resource]
     */
    def getResourcesByType(type) {
        def all = []
        resources.each { url, resource -> if (type == resource.class.simpleName) all.add([url, resource])}
        all
    }

    def resolveReference(URI referenceUrl) {
        resolveReference(fullUrl, referenceUrl, new ResolverConfig())
    }

    def resolveReference(URI containingUrl, URI referenceUrl) {
        resolveReference(containingUrl, referenceUrl, new ResolverConfig())
    }

    /**
     *
     * @param containingUrl  (fullUrl)
     * @param referenceUrl   (reference)
     * @return [url, Resource]
     */
    // TODO - needs toughening - containingURL could be null if referenceURL is absolute
    LoadedResource resolveReference(URI containingUrl, URI referenceUrl, ResolverConfig config) {
        assert referenceUrl, "Reference from ${containingUrl} is null"
        Val thisVal = topVal.addSection("Resolver: Resolve URL ${referenceUrl}... ${config}")

        if (config.containedRequired || (config.containedOk && referenceUrl.toString().startsWith('#'))) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith('#') && config.containedOk) {
                IBaseResource res = getContainedResource(referenceUrl)
                thisVal.msg("Resolver: ...contained")
                return new LoadedResource(referenceUrl, res)
            }
            return new LoadedResource(null, null)
        }
        if (!config.externalRequired) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith('#') && config.containedOk) {
                def res = getContainedResource(referenceUrl)
                thisVal.msg("Resolver: ...contained")
                return new LoadedResource(referenceUrl, res)
            }
            if (resources[referenceUrl]) {
                thisVal.msg("Resolver: ...in bundle")
                return new LoadedResource(referenceUrl, resources[referenceUrl])
            }
            def isRelativeReference = isRelative(referenceUrl)
            if (config.relativeReferenceRequired && !isRelativeReference) {
                thisVal.msg("Resolver: ...relative reference required - not relative")
                return new LoadedResource(null, null)
            }
            def type = resourceTypeFromUrl(referenceUrl)
            // TODO - isAbsolute does an assert on containingUrl... here is why... if we have gotten to this point...
            // Resource.fullUrl (containingUrl) is a uuid (not a real reference) then it is not absolute
            // if it is not absolute then this refernceUrl cannot be relative (relative to what???).
            // this is a correct validation but needs a lot more on the error message (now a Groovy assert)
            if (!isAbsolute(containingUrl) && isRelative(referenceUrl)) {
                def x = resources.find {
                    def key = it.key
                    // for Patient, it must be absolute reference
                    if ('Patient' == type && isRelativeReference && !config.relativeReferenceOk)
                        return false
                    key.toString().endsWith(referenceUrl.toString())
                }
                if (x) {
                    thisVal.msg("Resolver: ...found via relative reference")
                    return new LoadedResource(x.key, x.value)
                }
            }
            if (isAbsolute(containingUrl) && isRelative(referenceUrl)) {
                URI url = UriBuilder.rebase(containingUrl, referenceUrl)
                if (resources[url]) {
                    thisVal.msg("Resolver: ...found in bundle")
                    return new LoadedResource(url, resources[url])
                }
                if (resourceCacheMgr) {
                    thisVal.msg("Resolver: ...looking in Resource Cache")
                    def resource = resourceCacheMgr.getResource(url)
                    if (resource) {
                        thisVal.msg("Resolver: ...returned from cache")
                        return new LoadedResource(url, resource)
                    }
                } else
                    thisVal.msg("Resource Cache not configured")
            }
        }

        // external
        if (!config.internalRequired && isAbsolute(referenceUrl)) {
            if (resourceCacheMgr) {
                thisVal.msg("Resolver: ...looking in Resource Cache")
                IBaseResource resource = resourceCacheMgr.getResource(referenceUrl)
                if (resource) {
                    thisVal.msg("Resolver: ...returned from cache")
                    return new LoadedResource(referenceUrl, resource)
                }
            } else {
                thisVal.msg("Resource Cache not configured")
            }
            IBaseResource res = FhirClient.readResource(referenceUrl)
            if (res) {
                thisVal.msg("Resolver: ...found")
                return new LoadedResource(referenceUrl, res)
            } else {
                thisVal.msg("Resolver: ${referenceUrl} ...not available")
                return new LoadedResource(null, null)
            }
        }

        thisVal.err("Resolver: ...failed")
        new LoadedResource(null, null)
    }

    static URI rebase(URI containingUrl, referenceUrl) {
//        if (containingUrl) containingUrl = containingUrl.toString()
        if (referenceUrl) referenceUrl = referenceUrl.toString()
        UriBuilder.build(UriBuilder.getBase(containingUrl).toString() + '/' + referenceUrl)
    }

    static String resourceTypeFromUrl(URI fullUrl) {
        assert fullUrl
        UriBuilder.getResourceType(fullUrl)
    }

    static String resourceIdFromUrl(String fullUrl) {
        fullUrl.reverse().split('/')[0].reverse()
    }

    static URI relativeUrl(URI fullUrl) {
        UriBuilder.getRelative(fullUrl)
    }

    static String id(URI url) {
        UriBuilder.getId(url)
    }

    static boolean isRelative(URI url) {
        !isAbsolute(url)
    }

    static boolean isAbsolute(URI url) {
        assert url
        url.isAbsolute()
    }

}
