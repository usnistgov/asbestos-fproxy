package gov.nist.asbestos.fproxy.channels.mhd.resolver

class LegalId {

    static legalChars = ('a'..'z') + ('A'..'Z') + '-' + '.'

    static boolean isLegal(String id) {
        boolean legal = id.size() <= 64

        id.toCharArray().each {
            if (!legalChars.contains(it))
                legal = false
        }

        legal
    }
}
