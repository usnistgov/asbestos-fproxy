package gov.nist.asbestos.fproxy.channels.mhd.transactions


import gov.nist.asbestos.simapi.tk.stubs.ValidationContext
import spock.lang.Specification

class PnrTest extends Specification {

    def 'test' () {
        when:
        ValidationContext vc = new ValidationContext()
        vc.isPnR = true

        then:
        true
    }
}
