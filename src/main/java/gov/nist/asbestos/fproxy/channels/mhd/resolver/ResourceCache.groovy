package gov.nist.asbestos.fproxy.channels.mhd.resolver

import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import groovy.transform.TypeChecked;

@TypeChecked
interface ResourceCache {
    ResourceWrapper readResource(Ref url)
    void add(Ref ref, ResourceWrapper resource)
}
