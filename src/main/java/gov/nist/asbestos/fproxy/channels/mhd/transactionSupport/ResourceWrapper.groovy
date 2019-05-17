package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.simapi.validation.Val
import groovy.transform.TypeChecked
import org.hl7.fhir.dstu3.model.BaseResource
import org.hl7.fhir.dstu3.model.Resource

@TypeChecked
class ResourceWrapper {
    Resource resource
    String assignedId
    Ref fullUrl
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    Map<Ref, ResourceWrapper> contained = [:]


    ResourceWrapper(Resource resource) {
        this.resource = resource
    }

    ResourceWrapper setId(String id) {
        assignedId = id
        this
    }

    ResourceWrapper setFullUrl(Ref fullUrl) {
        this.fullUrl = fullUrl
        this
    }

    String getId() {
        if (fullUrl.id) return fullUrl.id
        if (assignedId) return assignedId
        assert false : "Cannot retreive id for ${resource}"
    }

    private boolean addContainedResource(ResourceWrapper resource, Val val) {
        Ref id = new Ref(resource.resource.id)
        boolean duplicate = contained.containsKey(id)
        if (!duplicate) contained[id] = resource
        if (duplicate)
            val.err(new Val().msg("Contained resource ${id} is a duplicate within ${resource.resource.id}"))
        return duplicate
    }
}
