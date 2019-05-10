package gov.nist.asbestos.fproxy.support

import gov.nist.asbestos.simapi.http.HttpBase
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost

interface BaseChannel extends ChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut)
    void transformRequest(HttpGet requestIn, HttpGet requestOut)
    URI transformRequestUrl(String endpoint, HttpBase requestIn)
    void transformResponse(HttpBase responseIn, HttpBase responseOut)
}
