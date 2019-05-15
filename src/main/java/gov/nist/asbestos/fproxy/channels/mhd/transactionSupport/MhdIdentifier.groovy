package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

import org.hl7.fhir.dstu3.model.Identifier

class MhdIdentifier {
    String system
    String value
    Identifier identifier

    MhdIdentifier(Identifier identifier) {
        this.identifier = identifier
        system = identifier.system
        value = identifier.value
    }
}
