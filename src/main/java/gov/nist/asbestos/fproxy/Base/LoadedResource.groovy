package gov.nist.asbestos.fproxy.Base

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper

class LoadedResource {

    Ref ref
    ResourceWrapper resource

    LoadedResource(Ref ref, ResourceWrapper resource) {
        this.ref = ref
        this.resource = resource
    }
}
