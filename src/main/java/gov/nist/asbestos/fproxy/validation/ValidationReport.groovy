package gov.nist.asbestos.fproxy.validation

import groovy.transform.TypeChecked

@TypeChecked
class ValidationReport {
    List<Val> vals = []

    ValidationReport add(Val val) {
        vals << val
        this
    }

    boolean hasErrors() {
        search(vals)
    }

    private boolean search(List<Val> vals) {
        boolean found = false

        vals.each { Val val ->
            if (val.hasErrors()) found = true
            found = found || search(val.children)
        }

        found
    }

    String toString() {
        StringBuilder buf = new StringBuilder()

        buf.append('Validation Report\n')

        vals.each { Val val ->
            buf.append(val.toString())
        }
        buf.append('Validation result: ')
        if (hasErrors())
            buf.append('Failure\n')
        else
            buf.append('Success\n')

        return buf.toString()
    }
}
