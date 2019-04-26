package gov.nist.asbestos.fproxy

import gov.nist.asbestos.simapi.sim.SimStore
import gov.nist.asbestos.simapi.tk.installation.Installation
import gov.nist.asbestos.simapi.tk.simCommon.SimId
import groovy.servlet.GroovyServlet
import groovy.transform.TypeChecked

import javax.servlet.ServletConfig
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@TypeChecked
class ProxyServlet extends GroovyServlet {
    File externalCache = null
    SimId simId = null
    String transaction = null
    String actor = null
    SimStore simStore

    void init(ServletConfig config) {
        println 'init done'
        super.init(config)
        externalCache = new File('/home/bill/ec')
        Installation.instance().externalCache = externalCache
    }

    void doPost(HttpServletRequest req, HttpServletResponse resp) {
        // typical URI is
        // for FHIR transactions
        // http://host:port/appContext/prox/simId/actor/transaction
        // for general stuff
        // http://host:port/appContext/prox/simId
        try {
            String uri = req.requestURI.toLowerCase()
            println "uri is ${uri}"
            parseUri(uri)
        } catch (AssertionError e) {
            resp.sendError(resp.SC_NOT_FOUND, e.message)
        }
    }

    void parseUri(String uri) {
        List<String> uriParts = uri.split('/') as List<String>

        if (uriParts.size() >= 2) {
            // /prox/simId
            if (uriParts[0] == 'prox') { // no appContext
                simId = SimId.buildFromRawId(uriParts[1])
                uriParts.remove(0)  // prox
                uriParts.remove(0)  // simId
            } else if (uriParts[1] == 'prox') { // has appContext
                simId = SimId.buildFromRawId(uriParts[2])
                uriParts.remove(0)  // appContext
                uriParts.remove(0)  // prox
                uriParts.remove(0)  // simId
            }
        }
        assert simId : "ProxyServlet: request to ${uri} - cannot find SimId\n"

        if (!uriParts.empty) {
            actor = uriParts[0]
            uriParts.remove(0)
        }
        if (!uriParts.empty) {
            transaction = uriParts[0]
            uriParts.remove(0)
        }

        // verify that sim exists
        simStore = new SimStore(externalCache, simId)
        simStore.getStore()  // exception if sim does not exist
    }
}
