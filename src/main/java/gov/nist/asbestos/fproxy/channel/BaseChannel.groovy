package gov.nist.asbestos.fproxy.channel

import gov.nist.asbestos.simapi.http.operations.HttpBase
import gov.nist.asbestos.simapi.http.operations.HttpGet
import gov.nist.asbestos.simapi.http.operations.HttpPost

interface BaseChannel extends ChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut)
    void transformRequest(HttpGet requestIn, HttpGet requestOut)
    URI transformRequestUrl(String endpoint, HttpBase requestIn)
    void transformResponse(HttpBase responseIn, HttpBase responseOut)
}
