package gov.nist.asbestos.fproxy.Base

import org.hl7.fhir.instance.model.api.IBaseResource

class LoadedResource {
    URI uri
    IBaseResource resource

    LoadedResource(URI uri, IBaseResource resource) {
        this.uri = uri
        this.resource = resource
    }
}
