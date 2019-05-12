package gov.nist.asbestos.fproxy.channels.passthrough

import gov.nist.asbestos.fproxy.channel.BaseChannel
import gov.nist.asbestos.fproxy.channel.ChannelConfig
import gov.nist.asbestos.fproxy.events.EventStore
import gov.nist.asbestos.simapi.http.operations.HttpBase
import gov.nist.asbestos.simapi.http.operations.HttpGet
import gov.nist.asbestos.simapi.http.operations.HttpPost
import gov.nist.asbestos.simapi.http.headers.HeaderBuilder
import gov.nist.asbestos.simapi.http.headers.Headers
import groovy.transform.TypeChecked

@TypeChecked
class PassthroughChannel implements BaseChannel {
    ChannelConfig channelConfig = null

    @Override
    void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig
    }

    @Override
    void teardown() {

    }

    @Override
    void validateConfig(ChannelConfig simConfig) {
    }

    @Override
    void handle(EventStore event) {

    }

    @Override
    void transformRequest(HttpPost requestIn, HttpPost requestOut) {
        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.request = requestIn.request
        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo
//        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo
//        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    URI transformRequestUrl(String endpoint, HttpBase requestIn) {
        assert channelConfig
        channelConfig.translateEndpointToFhirBase(requestIn.requestHeaders.pathInfo)
    }

    @Override
    void transformResponse(HttpBase responseIn, HttpBase responseOut) {
        responseOut.responseHeaders = responseIn.responseHeaders
        responseOut.response = responseIn.response
    }
}
