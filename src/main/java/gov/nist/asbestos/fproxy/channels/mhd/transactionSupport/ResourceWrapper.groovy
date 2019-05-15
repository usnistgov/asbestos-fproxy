package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RawSendRequest
import org.hl7.fhir.instance.model.api.IBaseResource

class ResourceWrapper {
    IBaseResource resource
    String assignedId
    String fullUrl

    ResourceWrapper(IBaseResource resource) {
        this.resource = resource
    }

    ResourceWrapper setId(String id) {
        assignedId = id
        this
    }

    ResourceWrapper setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl
        this
    }
}
