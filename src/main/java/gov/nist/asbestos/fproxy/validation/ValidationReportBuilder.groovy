package gov.nist.asbestos.fproxy.validation

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ValidationReportBuilder {

    static String toJson(ValidationReport report) {
        JsonOutput.toJson(report)
    }

    static ValidationReport fromJson(String json) {
        (ValidationReport) new JsonSlurper().parseText(json)
    }
}
