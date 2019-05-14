package gov.nist.asbestos.fproxy.channels.mhd.resolver

import spock.lang.Specification

class RefTest extends Specification {

    def 'getBase' () {
        setup:
        Ref result
        Ref theBase = new Ref('http://localhost:8999/fhir/x')

        when:
        result = new Ref('http://localhost:8999/fhir/x/Patient/2')

        then:
        result.base == theBase

        when:
        Ref ref = new Ref('http://localhost:8999/fhir/x/Patient/_history/1')

        then:
        theBase == ref.base

        when:
        Ref aBase = new Ref('http://myPatientServer:8999/fhir/x/Patient/_history/1').base
        Ref anotherBase = new Ref('http://myPatientServer:8999/fhir/x').base

        then:
        aBase == anotherBase
    }

    def 'getId' () {
        setup:
        String id

        when:
        id = new Ref('http://localhost:8999/fhir/x/Patient/2').id

        then:
        id == '2'

        when:
        id = new Ref('http://localhost:8999/fhir/x/Patient/2/_history/1').id

        then:
        id == '2'

        when:
        id = new Ref('http://localhost:8999/fhir/x/Patient').id

        then:
        id == null

        when:
        id = new Ref('http://localhost:8999/fhir/x').id

        then:
        id == null
    }

    def 'getResourceType' () {
        setup:
        String type

        when:
        type = new Ref('http://localhost:8999/fhir/x/Patient/2').resourceType

        then:
        type == 'Patient'

        when:
        type = new Ref('http://localhost:8999/fhir/x/Patient/2/_history/1').resourceType

        then:
        type == 'Patient'

        when:
        type = new Ref('http://localhost:8999/fhir/x/Patient').resourceType

        then:
        type == 'Patient'

        when:
        type = new Ref('http://localhost:8999/fhir/x').resourceType

        then:
        type == null
    }

    def 'getRelative' () {
        setup:
        Ref relative

        when:
        relative = new Ref('http://localhost:8080/fhir/x/Patient').relative

        then:
        relative == new Ref('Patient')

        when:
        relative = new Ref('http://localhost:8080/fhir/x/Patient/1').relative

        then:
        relative == new Ref('Patient/1')

        when:
        relative = new Ref('http://localhost:8080/fhir/x/Patient/1/_history/1').relative

        then:
        relative == new Ref('Patient/1/_history/1')

        when:
        relative = new Ref('http://localhost:8080/fhir/x').relative

        then:
        relative == new Ref('')
    }

    def 'withNewId' () {
        setup:
        Ref ref
        Ref newRef

        when:
        ref = new Ref('http://localhost:8080/fhir/x/Patient/1')
        newRef = ref.withNewId('2')

        then:
        newRef == new Ref('http://localhost:8080/fhir/x/Patient/2')

        when:
        ref = new Ref('http://localhost:8080/fhir/x')
        newRef = ref.withNewId('2')

        then:
        !newRef.absolute
    }

    def 'rebase' () {
        when:
        Ref orig = new Ref('http://localhost:8080/fhir/x/Patient/1')
        String newBase = 'https://example.com:9090/fhir'
        Ref rebased = orig.rebase(newBase)

        then:
        rebased.toString() == 'https://example.com:9090/fhir/Patient/1'
    }

    def 'version' () {
        when:
        Ref good = new Ref('http://localhost:8080/fhir/x/Patient/1/_history/9')

        then:
        good.version == '9'

        when:
        Ref bad = new Ref('http://localhost:8080/fhir/x/Patient/1')

        then:
        bad.version == null
    }

    def 'full' () {
        when:
        Ref ref = new Ref('http://localhost:8080/fhir/x/Patient/1/_history/9')

        then:
        ref.full == new Ref('http://localhost:8080/fhir/x/Patient/1')
        ref.absolute

        when:
        ref = new Ref('http://localhost:8080/fhir/x')

        then:
        ref.full == new Ref('http://localhost:8080/fhir/x')
        !ref.absolute

        when:
        ref = new Ref('http://localhost:8080/fhir/x/Patient')

        then:
        ref.full == new Ref('http://localhost:8080/fhir/x/Patient')
        !ref.absolute
    }

    def 'contained' () {
        when:
        Ref ref = new Ref('#h')

        then:
        ref.id == 'h'
    }
}
