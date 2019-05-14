package gov.nist.asbestos.fproxy.channels.mhd.resolver

import spock.lang.Specification

class LegalIdTest extends Specification {

    def 'numeric' () {
        when:
        String id = '4'

        then:
        LegalId.isLegal(id)
    }
}
