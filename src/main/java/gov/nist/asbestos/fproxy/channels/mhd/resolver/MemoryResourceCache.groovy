package gov.nist.asbestos.fproxy.channels.mhd.resolver

import org.hl7.fhir.instance.model.api.IBaseResource

class MemoryResourceCache implements ResourceCache {
    Map<Ref, IBaseResource> cache = [:]

    @Override
    IBaseResource readResource(Ref url) {
        cache[url]
    }

    @Override
    void add(Ref ref, IBaseResource resource) {
        cache[ref] = res
    }
}
