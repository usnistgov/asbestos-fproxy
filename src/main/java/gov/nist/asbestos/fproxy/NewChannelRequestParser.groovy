package gov.nist.asbestos.fproxy

import groovy.json.JsonSlurper

class NewChannelRequestParser {

    static NewChannelRequest parse(String request) {
        NewChannelRequest req = new NewChannelRequest()

        def object = new JsonSlurper().parseText(request)
        req.environment = object.environment
        req.testSession = object.testSession
        req.simId = object.simId
        req.actorType = object.actorType

        assert req.environment : "New Proxy Channel request - environment is null"
        assert req.testSession : "New Proxy Channel request - testSession is null"
        assert req.simId : "New Proxy Channel request - simId is null"

        req
    }
}
