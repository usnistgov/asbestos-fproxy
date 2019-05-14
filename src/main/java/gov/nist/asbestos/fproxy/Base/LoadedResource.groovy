package gov.nist.asbestos.fproxy.Base

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import org.hl7.fhir.instance.model.api.IBaseResource

class LoadedResource {

    Ref uri
    IBaseResource resource

    LoadedResource(Ref uri, IBaseResource resource) {
        this.uri = uri
        this.resource = resource
    }
}
