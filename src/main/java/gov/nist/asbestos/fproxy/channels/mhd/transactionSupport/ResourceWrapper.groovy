package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.simapi.validation.Val
import org.hl7.fhir.dstu3.model.BaseResource
import org.hl7.fhir.dstu3.model.DomainResource
import org.hl7.fhir.dstu3.model.Resource

class ResourceWrapper {
    Resource resource
    String assignedId
    Ref fullUrl
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    Map<String, BaseResource> contained = [:]


    ResourceWrapper(DomainResource resource, Val val) {
        this.resource = resource

        List<Resource> contained = resource.contained
        contained?.each { Resource r ->
            boolean duplicate = addContainedResource(r)
            if (duplicate)
                val.err(new Val()
                        .msg("Duplicate contained Resource (${r.id} in Resource ${resource.class.simpleName}/${resource.id})"))
        }
    }

    ResourceWrapper setId(String id) {
        assignedId = id
        this
    }

    ResourceWrapper setFullUrl(Ref fullUrl) {
        this.fullUrl = fullUrl
        this
    }

    private boolean addContainedResource(Resource resource) {
        String id = resource.id
        boolean duplicate = contained.containsKey(id)
        if (!duplicate) contained[id] = resource
        return duplicate
    }
}
