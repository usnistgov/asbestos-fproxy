package gov.nist.asbestos.fproxy.support

import gov.nist.asbestos.simapi.http.HttpGeneralDetails
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost

interface BaseChannel extends ChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut)
    void transformRequest(HttpGet requestIn, HttpGet requestOut)
    String transformRequestUrl(String endpoint, HttpGeneralDetails requestIn)
    void transformResponse(HttpGeneralDetails responseIn, HttpGeneralDetails responseOut)
}
