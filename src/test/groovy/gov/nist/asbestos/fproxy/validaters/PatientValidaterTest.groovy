package gov.nist.asbestos.fproxy.validaters

import gov.nist.asbestos.fproxy.validation.ValidationReport
import groovy.json.JsonSlurper
import spock.lang.Specification

class PatientValidaterTest extends Specification {

    def 'good' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(good))
        println report

        then:
        !report.hasErrors()
    }

    def 'missing identifier' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(missingIdentifier))
        println report

        then:
        report.hasErrors()
    }

    def 'no identifier' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(noIdentifier))
        println report

        then:
        report.hasErrors()
    }

    def 'missing system' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(missingSystem))
        println report

        then:
        report.hasErrors()
    }

    def 'some systems not an oid' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(someSystemNotOid))
        println report

        then:
        !report.hasErrors()
    }

    def 'no systems are oids' () {
        setup:
        ValidationReport report = new ValidationReport()

        when:
        new PatientValidater(report).run(new JsonSlurper().parseText(noSystemsAreOid))
        println report

        then:
        report.hasErrors()
    }


    def good = '''
{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.3000",
      "value": "IHEBLUE-2737"
    },
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.3000.1.6",
      "value": "IHEFACILITY-2737"
    },
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.2000",
      "value": "IHEGREEN-2737"
    },
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.1000",
      "value": "IHERED-2737"
    }
  ]
}
'''
    def missingIdentifier = '''
{
  "resourceType": "Patient",
  "identifier": [
  ]
}
'''
    def noIdentifier = '''
{
  "resourceType": "Patient",
}
'''
    def missingSystem = '''
{
  "resourceType": "Patient",
  "identifier": [
    {
      "value": "IHEBLUE-2737"
    }
  ]
}
'''
    def someSystemNotOid = '''
{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.3000",
      "value": "IHEBLUE-2737"
    },
    {
      "system": "1.3.6.1.4.1.21367.3000.1.6",
      "value": "IHEFACILITY-2737"
    },
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.2000",
      "value": "IHEGREEN-2737"
    },
    {
      "system": "urn:oid:1.3.6.1.4.1.21367.13.20.1000",
      "value": "IHERED-2737"
    }
  ]
}
'''
    def noSystemsAreOid = '''
{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "1.3.6.1.4.1.21367.13.20.3000",
      "value": "IHEBLUE-2737"
    },
    {
      "system": "1.3.6.1.4.1.21367.3000.1.6",
      "value": "IHEFACILITY-2737"
    },
    {
      "system": "1.3.6.1.4.1.21367.13.20.2000",
      "value": "IHEGREEN-2737"
    },
    {
      "system": "1.3.6.1.4.1.21367.13.20.1000",
      "value": "IHERED-2737"
    }
  ]
}
'''

}
