package gov.nist.asbestos.fproxy.channels.mhd.resolver

import spock.lang.Specification

class LegalIdTest extends Specification {

    def 'test' () {
        when:
        String id = '4'

        then:
        LegalId.isLegal(id)

        when:
        id = 'id3-4A.'

        then:
        LegalId.isLegal(id)
    }
}
