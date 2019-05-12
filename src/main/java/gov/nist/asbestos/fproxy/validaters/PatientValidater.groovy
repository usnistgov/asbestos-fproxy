package gov.nist.asbestos.fproxy.validaters

import gov.nist.asbestos.fproxy.validation.Val
import gov.nist.asbestos.fproxy.validation.ValidationReport


class PatientValidater {
    ValidationReport report

    PatientValidater(ValidationReport report) {
        this.report = report
    }

    def run(Map patient) {
        Val top  = new Val()

        if (patient.resourceType == 'Patient') {
            top.msg('Patient resource')
        } else {
            top.err('resourceType missing or not set to Patient')
        }

        //
        // has at least one identifier with system and value
        //
        List identifiers = patient.identifier
        if (identifiers) {
            report.add(top.msg("Has ${identifiers.size()} identifiers"))
            if (identifiers.size() == 0) {
                report.add(top.err('No identifiers'))
            }
        } else {
            report.add(top.err('No identifiers'))
        }
        if (!identifiers) return
        boolean hasProperIdentifier = false
        identifiers.each { identifier ->
            if (isOid(identifier.system) && identifier.value) {
                hasProperIdentifier = true
            }
        }
        if (hasProperIdentifier)
            top.msg('Has identifier with value and OID valued system')
        else
            top.err('Does not have an identifier with OID value system')
    }

    boolean isOid(String value) {
        value?.startsWith('urn:oid:')
    }
}
