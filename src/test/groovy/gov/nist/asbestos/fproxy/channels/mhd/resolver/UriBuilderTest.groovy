package gov.nist.asbestos.fproxy.channels.mhd.resolver

import spock.lang.Specification

class UriBuilderTest extends Specification {

    def 'getBase' () {
        setup:
        URI result
        URI base = new URI('http://localhost:8999/fhir/x')

        when:
        result = UriBuilder.getBase(new URI('http://localhost:8999/fhir/x/Patient/2'))

        then:
        result == base

        when:
        result = UriBuilder.getBase(base)

        then:
        result == base

        when:
        base = new URI('http://localhost:8999/fhir/x/Patient/_history/1')
        result = UriBuilder.getBase(base)

        then:
        result == new URI('http://localhost:8999/fhir/x')

        when:
        base = new URI('http://myPatientServer:8999/fhir/x/Patient/_history/1')
        result = UriBuilder.getBase(base)

        then:
        result == new URI('http://myPatientServer:8999/fhir/x')
    }

    def 'getId' () {
        setup:
        String id

        when:
        id = UriBuilder.getId(new URI('http://localhost:8999/fhir/x/Patient/2'))

        then:
        id == '2'

        when:
        id = UriBuilder.getId(new URI('http://localhost:8999/fhir/x/Patient/2/_history/1'))

        then:
        id == '2'

        when:
        id = UriBuilder.getId(new URI('http://localhost:8999/fhir/x/Patient'))

        then:
        id == null

        when:
        id = UriBuilder.getId(new URI('http://localhost:8999/fhir/x'))

        then:
        id == null
    }

    def 'getResourceType' () {
        setup:
        String type

        when:
        type = UriBuilder.getResourceType(new URI('http://localhost:8999/fhir/x/Patient/2'))

        then:
        type == 'Patient'

        when:
        type = UriBuilder.getResourceType(new URI('http://localhost:8999/fhir/x/Patient/2/_history/1'))

        then:
        type == 'Patient'

        when:
        type = UriBuilder.getResourceType(new URI('http://localhost:8999/fhir/x/Patient'))

        then:
        type == 'Patient'

        when:
        type = UriBuilder.getResourceType(new URI('http://localhost:8999/fhir/x'))

        then:
        type == null
    }

    def 'getRelative' () {
        setup:
        URI relative

        when:
        relative = UriBuilder.getRelative(new URI('http://localhost:8080/fhir/x/Patient'))

        then:
        relative.toString() == 'Patient'

        when:
        relative = UriBuilder.getRelative(new URI('http://localhost:8080/fhir/x/Patient/1'))

        then:
        relative.toString() == 'Patient/1'

        when:
        relative = UriBuilder.getRelative(new URI('http://localhost:8080/fhir/x/Patient/1/_history/1'))

        then:
        relative.toString() == 'Patient/1/_history/1'
    }

    def 'withNewId' () {
        setup:
        URI ref
        URI newRef

        when:
        ref = new URI('http://localhost:8080/fhir/x/Patient/1')
        newRef = UriBuilder.withNewId(ref, '2')

        then:
        newRef == new URI('http://localhost:8080/fhir/x/Patient/2')
    }

    def 'rebase' () {
        when:
        URI orig = new URI('http://localhost:8080/fhir/x/Patient/1')
        URI newBase = new URI('https://example.com:9090/fhir')
        URI rebased = UriBuilder.rebase(orig, newBase)

        then:
        rebased.toString() == 'https://example.com:9090/fhir/Patient/1'
    }
}
