package gov.nist.asbestos.fproxy.channels.mhd.resolver

import groovy.transform.TypeChecked
import org.apache.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource

/**
 * build by a factory - either TestResourceCacheFactory or ResourceCacheMgrFactory
 *
 * Manages multiple resource caches.  Each cache is identified by its BaseUrl
 * For now all caches are static - content loaded from maven build or from a test case
 */
@TypeChecked
class ResourceCacheMgr {
    private static final Logger logger = Logger.getLogger(ResourceCacheMgr.class)
    Map<Ref, ResourceCache> caches = [:]  // baseUrl -> cache
    File externalCache

    ResourceCacheMgr(File externalCache) {
        this.externalCache = externalCache
        if (externalCache)
            loadCache(new File(externalCache, 'fhirResourceCache'))
    }

    void loadCache(File cacheCollectionDir) {
        cacheCollectionDir.listFiles().each {File cache ->
            if (cache.isDirectory() && new File(cache, 'cache.properties').exists()) {
                logger.info("Scanning Resource Cache directory ${cache}")
                FileSystemResourceCache rcache = new FileSystemResourceCache(cache)
                caches[rcache.base] = rcache
            }
        }
    }

    void addMemoryCacheElement(Ref uri, IBaseResource resource) {
        assert uri.isAbsolute()
        Ref fhirbase = uri.base
        ResourceCache cache = caches[fhirbase]
        if (!cache) {
            cache = new MemoryResourceCache()
            caches[fhirbase] = cache
        }
        assert cache instanceof MemoryResourceCache
        cache.add(uri.relative, resource)
    }


    /**
     * return resource or throw exception
     * @param fullUrl
     * @return
     */
     IBaseResource getResource(Ref fullUrl) {
        assert fullUrl.isAbsolute()
        Ref baseUrl = fullUrl.base
        ResourceCache cache = caches[baseUrl]
        if (!cache) throw new Exception("Cannot access ${fullUrl}\nNo cache defined for baseUrl ${baseUrl}\nCaches exist for ${caches.keySet()}")
        return cache.readResource(fullUrl.relative)
    }

}
