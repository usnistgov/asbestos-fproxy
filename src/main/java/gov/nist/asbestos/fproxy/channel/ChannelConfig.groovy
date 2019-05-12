package gov.nist.asbestos.fproxy.channel

import groovy.transform.TypeChecked

@TypeChecked
class ChannelConfig {
    String environment
    String testSession
    String channelId
    String actorType
    String channelType
    String fhirBase
    Map extensions

    // TODO test needed
    URI translateEndpointToFhirBase(URI req) {
        String path  = req.path
        int channelI = path.indexOf('/Channel')
        if (channelI != -1) {
            int beyondChannelI = channelI + '/Channel'.size()
            path = "${fhirBase}${path.substring(beyondChannelI)}"
        }
        new URI(req.scheme, req.userInfo, req.host, req.port, path, req.query, req.fragment)
    }
}
