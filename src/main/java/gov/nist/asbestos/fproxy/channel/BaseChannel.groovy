package gov.nist.asbestos.fproxy.channel

import HttpBase
import HttpGet
import HttpPost

interface BaseChannel extends ChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut)
    void transformRequest(HttpGet requestIn, HttpGet requestOut)
    URI transformRequestUrl(String endpoint, HttpBase requestIn)
    void transformResponse(HttpBase responseIn, HttpBase responseOut)
}
