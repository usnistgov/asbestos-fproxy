package gov.nist.asbestos.fproxy.channels.mhd.resolver

import gov.nist.asbestos.fproxy.Base.Base
import gov.nist.asbestos.fproxy.Base.LoadedResource
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import gov.nist.asbestos.simapi.tk.util.UuidAllocator
import gov.nist.asbestos.simapi.validation.Val

import groovy.transform.TypeChecked
import org.apache.log4j.Logger
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBaseResource

/**
 *
 */
@TypeChecked
class ResourceMgr {
    static private final Logger logger1 = Logger.getLogger(ResourceMgr.class);
    Bundle bundle = null
    // Object is some Resource type
    Map<Ref, ResourceWrapper> resources = [:]   // url -> resource
    int newIdCounter = 1

    // for current resource
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    Map<String, IBaseResource> containedResources = [:]
    Ref fullUrl

    // resource cache mgr
    ResourceCacheMgr resourceCacheMgr = null

    Val val

    ResourceMgr(Bundle bundle, Val val) {
        this.bundle = bundle
        this.val = val
        if (bundle)
            parseBundle()
    }

    ResourceMgr(Map<Ref, Resource> resourceMap, Val validationReport) {
        this.val = validationReport
        validationReport.add(topVal)
        parseResourceMap(resourceMap)
    }

    ResourceMgr addResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr
        this
    }

    def parseResourceMap(Map<Ref, Resource> resourceMap) {
        resourceMap.each { Ref fullUrl, Resource res ->
            assignId(res)
            addResource(fullUrl, res)
        }
    }

    void parseBundle() {
        Val thisVal = val.addSection("Load Bundle...")
        bundle.getEntry().each { Bundle.BundleEntryComponent component ->
            if (component.hasResource()) {
                ResourceWrapper wrapper = new ResourceWrapper(component.resource)
                        .setId(allocationSymbolicId())
                        .setFullUrl(component.fullUrl)

                thisVal.add("...${component.fullUrl}")
                addResource(new Ref(component.fullUrl), wrapper)
            }
        }
    }

    def currentResource(Resource resource) {
        clearContainedResources()
        assert resource instanceof DomainResource
        def contained = resource.contained
        contained?.each { Resource r ->
            boolean duplicate = addContainedResource(r)
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
    void addResource(Ref url, ResourceWrapper wrapper) {
        boolean duplicate = resources.containsKey(url)
        if (duplicate)
            val.err(new Val()
                    .msg("Duplicate resource found in bundle for URL ${url}"))
        else
            resources[url] = wrapper
    }

    boolean addContainedResource(IBaseResource resource) {
        assert resource instanceof DomainResource
        String id = resource.id
        boolean duplicate = containedResources.containsKey(id)
        containedResources[id] = resource
        return duplicate
    }

    def clearContainedResources() {
        containedResources = [:]
    }

    IBaseResource getContainedResource(Ref id) {
        assert id
        return containedResources[id.id]
    }

    Ref url(resource) {
        resources.entrySet().find { Map.Entry entry ->
            entry.value == resource
        }?.key as Ref
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

    def resolveReference(Ref referenceUrl) {
        resolveReference(fullUrl, referenceUrl, new ResolverConfig())
    }

    def resolveReference(Ref containingUrl, Ref referenceUrl) {
        resolveReference(containingUrl, referenceUrl, new ResolverConfig())
    }

    /**
     *
     * @param containingUrl  (fullUrl)
     * @param referenceUrl   (reference)
     * @return [url, Resource]
     */
    // TODO - needs toughening - containingURL could be null if referenceURL is absolute
    LoadedResource resolveReference(Ref containingUrl, Ref referenceUrl, ResolverConfig config) {
        assert referenceUrl, "Reference from ${containingUrl} is null"
        Val thisVal = topVal.addSection("Resolver: Resolve URL ${referenceUrl}... ${config}")

        if (config.containedRequired || (config.containedOk && referenceUrl.id.startsWith('#'))) {
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
            def isRelativeReference = !referenceUrl.isAbsolute()
            if (config.relativeReferenceRequired && !isRelativeReference) {
                thisVal.msg("Resolver: ...relative reference required - not relative")
                return new LoadedResource(null, null)
            }
            String resourceType = referenceUrl.resourceType
            // TODO - isAbsolute does an assert on containingUrl... here is why... if we have gotten to this point...
            // Resource.fullUrl (containingUrl) is a uuid (not a real reference) then it is not absolute
            // if it is not absolute then this refernceUrl cannot be relative (relative to what???).
            // this is a correct validation but needs a lot more on the error message (now a Groovy assert)
            if (!containingUrl.isAbsolute() && !referenceUrl.isAbsolute()) {
                def x = resources.find {
                    def key = it.key
                    // for Patient, it must be absolute reference
                    if ('Patient' == resourceType && isRelativeReference && !config.relativeReferenceOk)
                        return false
                    key.toString().endsWith(referenceUrl.toString())
                }
                if (x) {
                    thisVal.msg("Resolver: ...found via relative reference")
                    return new LoadedResource(x.key, x.value)
                }
            }
            if (containingUrl.isAbsolute() && !referenceUrl.isAbsolute()) {
                Ref url = containingUrl.rebase(referenceUrl)
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
        if (!config.internalRequired && referenceUrl.isAbsolute()) {
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
            IBaseResource res = referenceUrl.load()
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
    int symbolicIdCounter = 1
    String allocationSymbolicId() {
        "ID${symbolicIdCounter++}"
    }

}
