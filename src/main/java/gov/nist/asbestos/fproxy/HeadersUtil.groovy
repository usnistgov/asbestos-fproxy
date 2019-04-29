package gov.nist.asbestos.fproxy

class HeadersUtil {

    static String headersAsString(Headers headers) {
        StringBuilder buf = new StringBuilder()


        Enumeration names = headers.names
        while (names.hasMoreElements()) {
            String name = names.nextElement()
            Enumeration values = headers.headers.get(name)
            while (values.hasMoreElements()) {
                String value = values.nextElement()
                buf << "${name}: ${value}\r\n"
            }
        }

        buf.toString()
    }
}
