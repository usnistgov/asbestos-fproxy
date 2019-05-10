package gov.nist.asbestos.fproxy.passthrough

import gov.nist.asbestos.fproxy.support.BaseChannel
import gov.nist.asbestos.fproxy.support.BasicChannel
import gov.nist.asbestos.simapi.http.HttpGeneralDetails
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost
import gov.nist.asbestos.simapi.sim.basic.EventStore
import gov.nist.asbestos.simapi.sim.basic.ChannelConfig
import gov.nist.asbestos.simapi.sim.headers.HeaderBuilder
import gov.nist.asbestos.simapi.sim.headers.Headers

class PassthroughChannel extends BasicChannel implements BaseChannel {
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
        basicValidateConfig(simConfig)
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
        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo
        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    String transformRequestUrl(String endpoint, HttpGeneralDetails requestIn) {
        assert channelConfig
        channelConfig.translateEndpointToFhirBase(requestIn.requestHeaders.pathInfo)
    }

    @Override
    void transformResponse(HttpGeneralDetails responseIn, HttpGeneralDetails responseOut) {
        responseOut.responseHeaders = responseIn.responseHeaders
        responseOut.response = responseIn.response
    }
}
