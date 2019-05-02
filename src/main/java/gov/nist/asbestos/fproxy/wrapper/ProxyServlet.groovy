package gov.nist.asbestos.fproxy.wrapper

import gov.nist.asbestos.adapter.HttpGet
import gov.nist.asbestos.adapter.StackTrace
import gov.nist.asbestos.simapi.sim.basic.Event
import gov.nist.asbestos.simapi.sim.basic.SimStore
import gov.nist.asbestos.simapi.sim.basic.SimStoreBuilder
import gov.nist.asbestos.simapi.sim.headers.HeaderBuilder
import gov.nist.asbestos.simapi.sim.headers.Headers
import gov.nist.asbestos.simapi.sim.headers.RawHeaders
import gov.nist.asbestos.simapi.tk.installation.Installation
import gov.nist.asbestos.simapi.tk.simCommon.SimId
import groovy.transform.TypeChecked
import org.apache.log4j.Logger

import javax.servlet.ServletConfig
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@TypeChecked
class ProxyServlet extends HttpServlet {
    static Logger log = Logger.getLogger(ProxyServlet);
    File externalCache = null

    void init(ServletConfig config) {
        super.init(config)
        log.debug 'init done'
        externalCache = new File('/home/bill/ec')
        Installation.instance().externalCache = externalCache
    }

    void doPost(HttpServletRequest req, HttpServletResponse resp) {

        // typical URI is
        // for FHIR transactions
        // http://host:port/appContext/prox/simId/actor/transaction
        // for general stuff
        // http://host:port/appContext/prox/simId
//        resp.sendError(resp.SC_BAD_GATEWAY,'done')
        try {
            String uri = req.requestURI.toLowerCase()
            log.debug "doPost ${uri}"

            SimStore simStore = parseUri(uri, req, resp, Verb.post)
            if (!simStore)
                return

            Event event = simStore.newEvent().selectRequest()
            logRequest(req, event, 'POST')

        } catch (AssertionError e) {
            String msg = "AssertionError: ${e.message}\n${StackTrace.stackTraceAsString(e)}"
            log.error(msg)
            resp.setStatus(resp.SC_FORBIDDEN)
            return
        } catch (Throwable t) {
            String msg = "${t.message}\n${StackTrace.stackTraceAsString(t)}"
            log.error(msg)
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR)
            return
        }
    }

    void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String uri = req.requestURI.toLowerCase()
        log.info "doGet ${uri}"

        try {
            SimStore simStore = parseUri(uri, req, resp, Verb.get)
            if (!simStore)
                return

            Event event = simStore.newEvent().selectRequest()
            logRequest(req, event, 'GET')

            HttpGet getter = new HttpGet()
            getter.get(req.requestURI, req.contentType)

            event.newTask()
            logResponse(getter, event, getter.status)

            resp.status = getter.status

            event.selectRequest()
            Headers headers = logResponse(getter, event, getter.status)

            resp.contentType = headers.contentType
            resp.outputStream.write(getter.response.bytes)
        } catch (AssertionError e) {
            String msg = "AssertionError: ${e.message}\n${StackTrace.stackTraceAsString(e)}"
            log.error(msg)
            resp.setStatus(resp.SC_FORBIDDEN)
            return
        } catch (Throwable t) {
            String msg = "${t.message}\n${StackTrace.stackTraceAsString(t)}"
            log.error(msg)
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR)
            return
        }
    }

    void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String uri = req.requestURI.toLowerCase()
            log.info "doDelete  ${uri}"
            parseUri(uri, req, resp, Verb.delete)
            resp.setStatus(resp.SC_OK)
        } catch (AssertionError e) {
            String msg = "AssertionError: ${e.message}\n${StackTrace.stackTraceAsString(e)}"
            log.error(msg)
            resp.setStatus(resp.SC_FORBIDDEN)
            return
        } catch (Throwable t) {
            String msg = "${t.message}\n${StackTrace.stackTraceAsString(t)}"
            log.error(msg)
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR)
            return
        }
    }

    /**
     * Event.task must be selected before calling
     * @param req
     * @param event
     * @param verb
     */
    static logRequest(HttpServletRequest req, Event event, String verb) {
        event.selectRequest()
        event.putRequestBody(req.inputStream.bytes)
        RawHeaders rawHeaders = new RawHeaders()
        rawHeaders.addNames(req.headerNames)
        rawHeaders.names.each { String name ->
            rawHeaders.addHeaders(name, req.getHeaders(name))
        }
        rawHeaders.uriLine = verb + ' ' + req.pathInfo
        event.putRequestHeader(rawHeaders)
    }

    /**
     * Event.task must be selected before calling
     * @param response
     * @param event
     */
    static Headers logResponse(HttpGet getter, Event event, int status) {
        event.putResponseBody(getter.response.bytes)
        Headers headers = HeaderBuilder.parseHeaders(getter.requestHeaders)
        headers.status = status
        event.putResponseHeader(headers)
        headers
    }

    /**
     *
     * @param uri
     * @param req
     * @param resp
     * @param isDelete
     * @return SimStore
     */
    SimStore parseUri(String uri, HttpServletRequest req, HttpServletResponse resp, Verb verb) {
        List<String> uriParts = uri.split('/') as List<String>
        SimStore simStore = new SimStore(externalCache)

        if (uriParts.size() == 3 && uriParts[2] == 'prox' && verb != Verb.delete) {
            // CREATE
            // /appContext/prox
            // control channel - request to create proxy channel

            String rawRequest = req.inputStream.text
            log.debug rawRequest
            simStore = SimStoreBuilder.builder(externalCache, SimStoreBuilder.buildSimConfig(rawRequest))
            resp.setStatus((simStore.newlyCreated ? resp.SC_CREATED : resp.SC_OK))
            return simStore
        }

        if (uriParts.size() >= 4) {
            // /appContext/prox/simId
            if (uriParts[0] == '' && uriParts[2] == 'prox') { // no appContext
                SimId simId = SimId.buildFromRawId(uriParts[3])
                simStore = SimStoreBuilder.loader(externalCache, simId.testSession, simId.id)
                uriParts.remove(0)  // leasing empty string
                uriParts.remove(0)  // appContext
                uriParts.remove(0)  // prox
                uriParts.remove(0)  // simId
            }
        }

        assert simStore.simId : "ProxyServlet: request to ${uri} - SimId must be present in URI\n"

        if (verb == Verb.delete) {
            simStore.deleteSim()
            return simStore
        }

        if (!uriParts.empty) {
            simStore.actor = uriParts[0]
            uriParts.remove(0)
        }
        if (!uriParts.empty) {
            simStore.transaction = uriParts[0]
            uriParts.remove(0)
        }

        if (!simStore.actor)
            simStore.actor = 'fhir'
        if (!simStore.transaction)
            simStore.transaction = 'post'

        // verify that sim exists
        simStore.getStore()  // exception if sim does not exist

        return simStore // expect content

    }

    enum Verb {get, post, delete}

}
