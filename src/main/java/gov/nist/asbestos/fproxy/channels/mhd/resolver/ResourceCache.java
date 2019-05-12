package gov.nist.asbestos.fproxy.channels.mhd.resolver;

import org.hl7.fhir.instance.model.api.IBaseResource;

import java.net.URI;

public interface ResourceCache {
    IBaseResource readResource(URI url);
}
