package gov.nist.asbestos.fproxy

import groovy.servlet.GroovyServlet

import javax.servlet.ServletConfig
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ProxyServlet extends GroovyServlet {

    void init(ServletConfig config) {
        super.init(config)
    }

    void doPost(HttpServletRequest req, HttpServletResponse resp) {

    }
}