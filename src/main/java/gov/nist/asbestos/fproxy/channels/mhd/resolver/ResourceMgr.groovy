package gov.nist.asbestos.fproxy.channels.mhd.resolver

import gov.nist.asbestos.simapi.tk.util.UuidAllocator
import groovy.transform.TypeChecked
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
    Map<URI, Resource> containedResources = [:]
    URI fullUrl

    // resource cache mgr
    ResourceCacheMgr resourceCacheMgr = null

    ErrorRecorder er = null;

    class InternalLogger {
        ErrorRecorder er
        Logger logger

        InternalLogger(ErrorRecorder er, Logger logger) {
            this.er = er
            this.logger = logger
        }
        def info(String msg) {
            logger.info(msg)
            er?.detail(msg)
        }
        def error(String msg) {
            logger.error(msg)
            er?.err(XdsErrorCode.Code.NoCode, msg, "", "")
        }
        def warn(String msg) {
            logger.warn(msg)
            er?.warning(XdsErrorCode.Code.NoCode, msg, "", "")
        }
        def fatal(String msg) {
            logger.fatal(msg)
            er?.err(XdsErrorCode.Code.NoCode, msg, "", "")
        }
    }

    InternalLogger logger


    ResourceMgr() {
        er = null
        logger = new InternalLogger(er, logger1)
    }

    ResourceMgr(Bundle bundle, ErrorRecorder er) {
        this.bundle = bundle
        this.er = er;
        logger = new InternalLogger(er, logger1)
        if (bundle)
            parseBundle()
    }

    ResourceMgr(Map<URI, IBaseResource> resourceMap, ErrorRecorder er) {
        this.er = er;
        logger = new InternalLogger(er, logger1)
        parseResourceMap(resourceMap)
    }

    def addResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr
    }

    def parseResourceMap(Map<URI, IBaseResource> resourceMap) {
        resourceMap.each { URI fullUrl, IBaseResource res ->
            assignId(res)
            addResource(fullUrl, res)
        }
    }

    def parseBundle() {
        logger.info("Load Bundle...")
        bundle.getEntry().each { Bundle.BundleEntryComponent component ->
            if (component.hasResource()) {
                assignId(component.getResource())
                logger.info("...${component.fullUrl}")
                def duplicate = addResource(component.fullUrl, component.getResource())
                assert !duplicate, "Duplicate entry in bundle - URL is ${component.fullUrl}"
            }
        }
        if (er) {
            er.sectionHeading('Load Resources')
            er.detail(toString())
        }
    }

    def currentResource(resource) {
        clearContainedResources()
        assert resource instanceof DomainResource
        def contained = resource.contained
        contained?.each { Resource r ->
            def duplicate = addContainedResource(r)
            assert !duplicate, "Duplicate contained Resource (${r.id} in Resource ${resource.id})"
        }

        fullUrl = url(resource)
    }

    def hexChars = ('0'..'9') + ('a'..'f')
    boolean isUUID(String u) {
        if (u.startsWith('urn:uuid:')) return true
        try {
            int total = 0
            total += (0..7).sum { (hexChars.contains(u[it])) ? 0 : 1 }
            total += (9..12).sum { (hexChars.contains(u[it])) ? 0 : 1 }
            total += (14..17).sum { (hexChars.contains(u[it])) ? 0 : 1 }
            total += (19..22).sum { (hexChars.contains(u[it])) ? 0 : 1 }
            total += (24..35).sum { (hexChars.contains(u[it])) ? 0 : 1 }
            total += (u[8]) ? 0 : 1
            total += (u[13]) ? 0 : 1
            total += (u[18]) ? 0 : 1
            total += (u[23]) ? 0 : 1
            return total == 0
        } catch (Exception e) {
            return false
        }
    }

    String assignId(Resource resource) {
        if (!resource.id || isUUID(resource.id)) {
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
    boolean addResource(url, IBaseResource resource) {
        if (url instanceof String)
            url = UriBuilder.build(url)
        boolean duplicate = resources.containsKey(url)
        resources[url] = resource
        return duplicate
    }

    def addContainedResource(resource) {
        assert resource instanceof DomainResource
        def id = UriBuilder.build(resource.id)
        boolean duplicate = containedResources.containsKey(id)
        containedResources[id] = resource
        return duplicate
    }

    def clearContainedResources() {
        containedResources = [:]
    }

    def getContainedResource(id) {
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

    def resolveReference(containingUrl, referenceUrl) {
        resolveReference(containingUrl, referenceUrl, new ResolverConfig())
    }

    /**
     *
     * @param containingUrl  (fullUrl)
     * @param referenceUrl   (reference)
     * @return [url, Resource]
     */
    // TODO - needs toughening - containingURL could be null if referenceURL is absolute
    def resolveReference(containingUrl, referenceUrl, ResolverConfig config) {
        assert referenceUrl, "Reference from ${containingUrl} is null"
        logger.info("Resolver: Resolve URL ${referenceUrl}... ${config}")

        if (containingUrl instanceof String)
            containingUrl = UriBuilder.build(containingUrl)
        if (referenceUrl && (referenceUrl instanceof String))
            referenceUrl = UriBuilder.build(referenceUrl)

        if (config.containedRequired || (config.containedOk && referenceUrl.toString().startsWith('#'))) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith('#') && config.containedOk) {
                def res = getContainedResource(referenceUrl)
                logger.info("Resolver: ...contained")
                return [referenceUrl, res]
            }
            return [null, null]
        }
        if (!config.externalRequired) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith('#') && config.containedOk) {
                def res = getContainedResource(referenceUrl)
                def val = [referenceUrl, res]
                logger.info("Resolver: ...contained")
                return val
            }
            if (resources[referenceUrl]) {
                logger.info("Resolver: ...in bundle")
                return [referenceUrl, resources[referenceUrl]]
            }
            def isRelativeReference = isRelative(referenceUrl)
            if (config.relativeReferenceRequired && !isRelativeReference) {
                logger.warn("Resolver: ...relative reference required - not relative")
                return [null, null]
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
                    logger.info("Resolver: ...found via relative reference")
                    return [x.key, x.value]
                }
            }
            if (isAbsolute(containingUrl) && isRelative(referenceUrl)) {
                URI url = rebase(containingUrl, referenceUrl)
                if (resources[url]) {
                    logger.info("Resolver: ...found in bundle")
                    return [url, resources[url]]
                }
                if (resourceCacheMgr) {
                    logger.info("Resolver: ...looking in Resource Cache")
                    def resource = resourceCacheMgr.getResource(url)
                    if (resource) {
                        logger.info("Resolver: ...returned from cache")
                        return [url, resource]
                    }
                } else
                    logger.info("Resource Cache not configured")
            }
        }

        // external
        if (!config.internalRequired && isAbsolute(referenceUrl)) {
            if (resourceCacheMgr) {
                logger.info("Resolver: ...looking in Resource Cache")
                def resource
                try {
                    resource = resourceCacheMgr.getResource(referenceUrl)
                } catch (Exception e) {
                    ;
                }
                if (resource) {
                    logger.info("Resolver: ...returned from cache")
                    return [referenceUrl, resource]
                }
            } else
                logger.info("Resource Cache not configured")
            try {
                IBaseResource res
                try {
                    res = FhirClient.readResource(referenceUrl)
                } catch (Exception e) {
                    ;
                }
                if (res) {
                    logger.info("Resolver: ...found")
                    return [referenceUrl, res]
                }
            }
            catch (Exception e) {
                logger.warn("Resolver: ${referenceUrl} ...not available")
            }
        }

        logger.warn("Resolver: ...failed")
        [null, null]
    }

    static URI rebase(URI containingUrl, referenceUrl) {
//        if (containingUrl) containingUrl = containingUrl.toString()
        if (referenceUrl) referenceUrl = referenceUrl.toString()
        UriBuilder.build(baseUrlFromUrl(containingUrl).toString() + '/' + referenceUrl)
    }

    static String resourceTypeFromUrl(fullUrl) {
        assert fullUrl
        fullUrl = fullUrl.toString()
        if (fullUrl.startsWith('#')) return null
        fullUrl.reverse().split('/')[1].reverse()
    }

    static String resourceIdFromUrl(String fullUrl) {
        fullUrl.reverse().split('/')[0].reverse()
    }

    static URI relativeUrl(fullUrl) {
        fullUrl = fullUrl.toString()
        String[] parts = fullUrl.split('/')
        UriBuilder.build([parts[parts.size() - 2], parts[parts.size() - 1]].join('/'))
    }

    static id(url) {
        url = url.toString()
        String[] parts = url.split('/')
        return parts[parts.size() - 1]
    }

    static withNewId(fullUrl, newid) {
        def base = baseUrlFromUrl(fullUrl)
        def rel = relativeUrl(fullUrl)
        def type = resourceTypeFromUrl(fullUrl)
        return base + '/' + type + '/' + newid
    }

    /**
     * strips off id and resource type
     * @param fullUrl - fhirBase + ResourceType + id
     * @return
     */
    static URI baseUrlFromUrl(URI fullUrl) {
        if (fullUrl.toString().startsWith('urn')) return fullUrl
        assert fullUrl.toString().startsWith('http')
        List<String> parts = fullUrl.toString().split('/') as List<String>
        parts.remove(parts.size() - 1)
        parts.remove(parts.size() - 1)
        UriBuilder.build(parts.join('/'))
    }

    static boolean isRelative(url) {
        assert url
        url = url.toString()
        url && !url.startsWith('http') && url.split('\\/').size() ==2
    }

    static boolean isAbsolute(url) {
        assert url
        url.toString().startsWith('http')
    }

    /**
     *
     * @param id
     * @return [Resource]
     */
    def resolveId(id) {
        assert id
        return resources.values().find { it.id == id }
    }

}
