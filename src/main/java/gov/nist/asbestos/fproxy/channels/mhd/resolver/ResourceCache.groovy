package gov.nist.asbestos.fproxy.channels.mhd.resolver

import groovy.transform.TypeChecked;
import org.hl7.fhir.instance.model.api.IBaseResource;

@TypeChecked
interface ResourceCache {
    IBaseResource readResource(Ref url)
    void add(Ref ref, IBaseResource resource)
}
