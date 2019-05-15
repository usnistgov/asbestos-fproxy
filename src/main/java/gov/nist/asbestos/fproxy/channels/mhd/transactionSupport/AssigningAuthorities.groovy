package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

class AssigningAuthorities {
    List<String> values = []
    boolean any = false
    private static oidPrefix = 'urn:oid:'

    AssigningAuthorities allowAny() {
        any = true
        this
    }

    AssigningAuthorities addAuthority(String value) {
        if (value.startsWith(oidPrefix))
            value = value.substring(oidPrefix.size())
        values << value
        this
    }
}
