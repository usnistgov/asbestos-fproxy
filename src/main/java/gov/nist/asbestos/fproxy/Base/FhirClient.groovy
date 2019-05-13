package gov.nist.asbestos.fproxy.Base

import gov.nist.asbestos.simapi.http.operations.HttpGet
import groovy.transform.TypeChecked
import org.hl7.fhir.instance.model.api.IBaseResource

@TypeChecked
class FhirClient {

    static IBaseResource readResource(URI uri) {
        readResource(uri, FhirContentType.JSON)
    }

    static IBaseResource readResource(URI uri, FhirContentType fhirContentType) {
        HttpGet getter = new HttpGet()
        getter.get(uri, fhirContentType.contentType)
        parse(getter.responseText, fhirContentType)
    }

    static IBaseResource parse(String resourceText, FhirContentType fhirContentType) {
        if(!resourceText) return null
        (fhirContentType.isJson) ?
                Base.fhirContext.newJsonParser().parseResource(resourceText) :
                Base.fhirContext.newXmlParser().parseResource(resourceText)
    }
}
