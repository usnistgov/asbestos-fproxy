package gov.nist.asbestos.fproxy.wrapper

import gov.nist.asbestos.adapter.StackTrace
import gov.nist.asbestos.simapi.http.Gzip
import gov.nist.asbestos.simapi.http.HttpGeneralRequest
import gov.nist.asbestos.simapi.http.HttpGet
import gov.nist.asbestos.simapi.http.HttpPost
import gov.nist.asbestos.simapi.sim.basic.Event
import gov.nist.asbestos.simapi.sim.basic.SimStore
import gov.nist.asbestos.simapi.sim.basic.SimStoreBuilder
import gov.nist.asbestos.simapi.sim.basic.Task
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

            HttpPost requestIn = new HttpPost()

            Event event = simStore.newEvent()
            logRequestIn(event, requestIn, req, Verb.POST)

            log.info "=> ${simStore.endpoint} ${event.requestHeaders.contentType}"

            Task backSideTask = event.newTask()

            HttpGeneralRequest requestOut = transformRequest(backSideTask, requestIn)
            requestOut.url = transformRequestUrl(backSideTask, requestIn)

            requestOut.run()
            log.info "==> ${requestOut.status} ${(requestOut.response) ? requestOut.responseContentType : 'NULL'}"

            HttpGeneralRequest responseOut = transformResponse(event.selectTask(Task.REQUEST_TASK), requestOut)

            responseOut.responseHeaders.getAll().each { String name, String value ->
                resp.addHeader(name, value)
            }
            if (responseOut.response) {
                resp.outputStream.write(responseOut.response)
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

            HttpGet requestIn = new HttpGet()

            Event event = simStore.newEvent()
            logRequestIn(event, requestIn, req, Verb.GET)
            //requestIn.requestHeaders.verb = Verb.GET
            //requestIn.requestHeaders.pathInfo = req.requestURI

            // key request headers
            // accept-encoding: gzip
            // accept: *

            log.info "=> ${simStore.endpoint} ${event.requestHeaders.accept}"

            Task backSideTask = event.newTask()

            HttpGeneralRequest requestOut = transformRequest(backSideTask, requestIn)
            requestOut.url = transformRequestUrl(backSideTask, requestIn)

            requestOut.run()
            backSideTask.select()
            backSideTask.event.responseHeaders = requestOut.responseHeaders
            logResponseBody(backSideTask, requestOut)

            log.info "==> ${requestOut.status} ${(requestOut.response) ? requestOut.responseContentType : 'NULL'}"

            HttpGeneralRequest responseOut = transformResponse(event.selectTask(Task.REQUEST_TASK), requestOut)

            responseOut.responseHeaders.getAll().each { String name, String value ->
                resp.addHeader(name, value)
            }
            if (responseOut.response) {
                resp.outputStream.write(responseOut.response)
            }


//            getter.get(simStore.endpoint, event.requestHeaders.getMultiple(['accept']))
//            log.info "==> ${getter.status} ${(getter.response) ? getter.responseContentType : 'NULL'}"
//            logOperation(event, getter)
//            logResponse(event, getter)
//
//
//            Headers headers = HeaderBuilder.parseHeaders(getter.responseHeaders)
//            headers.getAll().each { String name, String value ->
//                resp.addHeader(name, value)
//            }
//            if (getter.response) {
//                resp.outputStream.write(getter.response)
//            }
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

    static HttpGeneralRequest logRequestIn(Event event, HttpGeneralRequest http, HttpServletRequest req, Verb verb) {
        event.selectRequest()

        // Log Headers
        RawHeaders rawHeaders = new RawHeaders()
        rawHeaders.addNames(req.headerNames)
        rawHeaders.names.each { String name ->
            rawHeaders.addHeaders(name, req.getHeaders(name))
        }
        rawHeaders.uriLine = "${verb} ${req.pathInfo}"
        Headers headers = HeaderBuilder.parseHeaders(rawHeaders)

        event.putRequestHeader(headers)
        http.requestHeaders = headers

        // Log body of POST
        if (verb == Verb.POST) {
            logRequestBody(event, headers, http, req)
        }

        http
    }

//    static logOperation(Event event, HttpGeneralRequest http) {
//        event.newTask()
//        event.putRequestHeader(HeaderBuilder.parseHeaders(http.requestHeaders))
//        logResponseDetails(event, http)
//    }

//    static logResponse(Event event, HttpGeneralRequest http) {
//        event.selectRequest()
//        logResponseDetails(event, http)
//    }

//    // TODO remove header processing?
//    static logResponseDetails(Event event, HttpGeneralRequest http) {
//        Headers hdrs = HeaderBuilder.parseHeaders(http.getResponseHeaders())
//        event.putResponseHeader(hdrs)
//        String encoding = hdrs.getContentEncoding()
//        if (http.response) {
//            if (encoding == 'gzip') {
//                String txt = Gzip.unzipWithoutBase64(http.response)
//                event.putResponseBodyText(txt)
//            } else if (http.responseContentType == 'text/html') {
//                event.putResponseHTMLBody(http.response)
//            }
//            event.putResponseBody(http.response)
//            if (event) return
//        }
//    }

    static logRequestBody(Event event, Headers headers, HttpGeneralRequest http, HttpServletRequest req) {
        byte[] bytes = req.inputStream.bytes
        event.putRequestBody(bytes)
        http.request = bytes
        String encoding = headers.getContentEncoding()
        if (encoding == 'gzip') {
            String txt = Gzip.unzipWithoutBase64(bytes)
            event.putRequestBodyText(txt)
            http.requestText = txt
        } else if (headers.contentType == 'text/html') {
            event.putRequestHTMLBody(bytes)
            http.requestText = new String(bytes)
        } else {
            http.requestText = new String(bytes)
        }
    }

    static logResponseBody(Task task, HttpGeneralRequest http) {
        task.select()
        Headers headers = http.responseHeaders
        byte[] bytes = http.response
        task.event.putResponseBody(bytes)
        String encoding = headers.getContentEncoding()
        if (encoding == 'gzip') {
            String txt = Gzip.unzipWithoutBase64(bytes)
            task.event.putResponseBodyText(txt)
            http.responseText = txt
        } else if (headers.contentType == 'text/html') {
            task.event.putResponseHTMLBody(bytes)
            http.responseText = new String(bytes)
        } else {
            http.responseText = new String(bytes)
        }

    }

//    static logRequestxBody(Event event, Headers headers, HttpServletRequest req) {
////        Headers hdrs = HeaderBuilder.parseHeaders(http.getRequestHeaders())
////        event.putRequestHeader(hdrs)
//        String encoding = headers.getContentEncoding()
//        if (http.request) {
//            if (encoding == 'gzip') {
//                String txt = Gzip.unzipWithoutBase64(http.request)
//                event.putRequestBodyText(txt)
//            } else if (http.requestContentType == 'text/html') {
//                event.putRequestHTMLBody(http.request)
//            }
//            event.putRequestBody(http.request)
//        }
//    }



    //
    // These  (transform*) are specific to a type of channel
    //

    static HttpGeneralRequest transformRequest(Task task, HttpPost requestIn) {
        HttpPost requestOut = new HttpPost()

        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.request = requestIn.request

        task.select()
        task.event.putRequestHeader(thruHeaders)
        task.event.putRequestBody(requestIn.request)

        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo

        requestOut
    }

    static HttpGeneralRequest transformRequest(Task task, HttpGet requestIn) {
        HttpGet requestOut = new HttpGet()

        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders

        task.select()
        task.event.putRequestHeader(thruHeaders)

        requestOut
    }

    static String transformRequestUrl(Task task, HttpGeneralRequest requestIn) {
        task.event.simStore.endpoint
    }

    static HttpGeneralRequest transformResponse(Task task, HttpGeneralRequest responseIn) {
        HttpGeneralRequest responseOut = new HttpGet()  // here GET vs POST does not matter

        responseOut.responseHeaders = responseIn.responseHeaders
        responseOut.response = responseIn.response

        task.select()
        task.event.putResponseHeader(responseIn.responseHeaders)
        task.event.putResponseBody(responseIn.response)

        responseOut
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
