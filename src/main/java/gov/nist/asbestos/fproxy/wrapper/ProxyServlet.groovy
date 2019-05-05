package gov.nist.asbestos.fproxy.wrapper

import gov.nist.asbestos.adapter.StackTrace
import gov.nist.asbestos.simapi.http.Gzip
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.sim.basic.Event
import gov.nist.asbestos.simapi.sim.basic.SimStore
import gov.nist.asbestos.simapi.sim.basic.SimStoreBuilder
import gov.nist.asbestos.simapi.sim.basic.Verb
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
            SimStore simStore = parseUri(uri, req, resp, Verb.POST)
            if (!simStore)
                return

            Event event = simStore.newEvent()
            logRequest(event, req, Verb.POST)
            log.info 'OK'

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
            parseUri(uri, req, resp, Verb.DELETE)
            resp.setStatus(resp.SC_OK)
            log.info 'OK'
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
        try {
            String uri = req.requestURI.toLowerCase()
            log.info "doGet ${uri}"
            SimStore simStore = parseUri(uri, req, resp, Verb.GET)
            if (!simStore)
                return

            Event event = simStore.newEvent()
            logRequest(event, req, Verb.GET)

            // key request headers
            // accept-encoding: gzip
            // accept: *

            log.info "=> ${simStore.endpoint} ${event.requestHeaders.accept}"
            HttpGet getter = new HttpGet()
            getter.get(simStore.endpoint, event.requestHeaders.getMultiple('accept'))
            //getter.get(simStore.endpoint, event.requestHeaders.getAll('accept'), event.requestHeaders.getAll('accept-encoding'))
            log.info "==> ${getter.status} ${(getter.response) ? getter.responseContentType : 'NULL'}"
            logGetRequest(event, getter)
            logResponse(event, getter)

            if (getter.response) {
                resp.contentType = getter.responseContentType
                resp.outputStream.write(getter.response)
            }
            log.info 'OK'
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

    def logRequest(Event event, HttpServletRequest req, Verb verb) {
        event.selectRequest()
        event.putRequestBody(req.inputStream.bytes)
        RawHeaders rawHeaders = new RawHeaders()
        rawHeaders.addNames(req.headerNames)
        rawHeaders.names.each { String name ->
            rawHeaders.addHeaders(name, req.getHeaders(name))
        }
        rawHeaders.uriLine = "${verb} ${req.pathInfo}"
        event.putRequestHeader(rawHeaders)
    }

    def logGetRequest(Event event, HttpGet getter) {
        event.newTask()
        event.putRequestHeader(HeaderBuilder.parseHeaders(getter.requestHeaders))
        if (getter.responseHeaders)
            event.putResponseHeader(HeaderBuilder.parseHeaders(getter.responseHeaders))
        if (getter.response)
            event.putResponseBody(getter.response)
    }

    def logResponse(Event event, HttpGet getter) {
        event.selectRequest()
        if (getter.responseHeaders)
            event.putResponseHeader(HeaderBuilder.parseHeaders(getter.responseHeaders))
        if (getter.response) {
            if (getter.responseContentType == 'text/html')
                event.putResponseHTMLBody(getter.response)
            else {
                event.putResponseBody(getter.response)
                Headers hdrs = HeaderBuilder.parseHeaders(getter.getResponseHeaders())
                String encoding = hdrs.getContentEncoding()
                if (encoding == 'gzip') {
                    String txt = Gzip.unzipWithoutBase64(getter.response)
                    event.putResponseBodyText(txt)
                }
            }
        }
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

        if (uriParts.size() == 3 && uriParts[2] == 'prox' && verb != Verb.DELETE) {
            // CREATE
            // /appContext/prox
            // control channel - request to create proxy channel

            String rawRequest = req.inputStream.text
            log.debug "CREATESIM ${rawRequest}"
            simStore = SimStoreBuilder.builder(externalCache, SimStoreBuilder.buildSimConfig(rawRequest))
            resp.setStatus((simStore.newlyCreated ? resp.SC_CREATED : resp.SC_OK))
            return null
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

        if (verb == Verb.DELETE) {
            simStore.deleteSim()
            return null
        }

        //simStore.actor = 'actor'

        if (!uriParts.empty) {
            simStore.resource = uriParts[0]
            uriParts.remove(0)
        }

        // verify that sim exists
        simStore.getStore()  // exception if sim does not exist

        log.debug "Sim ${simStore.simId} ${simStore.actor} ${simStore.resource}"

        return simStore // expect content

    }



}
