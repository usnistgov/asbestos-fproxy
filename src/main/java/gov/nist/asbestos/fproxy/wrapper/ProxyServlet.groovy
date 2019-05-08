package gov.nist.asbestos.fproxy.wrapper

import gov.nist.asbestos.adapter.StackTrace
import gov.nist.asbestos.fproxy.passthrough.PassthroughChannel
import gov.nist.asbestos.fproxy.support.BaseChannel
import gov.nist.asbestos.simapi.http.Gzip
import gov.nist.asbestos.simapi.http.HttpGeneralDetails
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

    Map proxyMap = [
            'passthrough': new PassthroughChannel()
    ]

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
            String uri = req.requestURI
            log.debug "doPost ${uri}"
            SimStore simStore = parseUri(uri, req, resp, Verb.POST)
            if (!simStore)
                return

            String channelType = simStore.config.channelType
            assert channelType : "Sim ${simStore.simId} does not define a Channel Type."
            BaseChannel channel = (BaseChannel) proxyMap.get(channelType)
            assert channel : "Cannot create Channel of type ${channelType}"

            HttpPost requestIn = new HttpPost()

            Event event = simStore.newEvent()
            // request from and response to client
            Task clientTask = event.store.selectClientTask()

            // log input from client
            logRequestIn(event, requestIn, req, Verb.POST)

            log.info "=> ${simStore.endpoint} ${event.store.requestHeader.contentType}"

            // interaction between proxy and target service
            Task backSideTask = event.store.newTask()

            // transform input request for backend service
            HttpGeneralDetails requestOut = transformRequest(backSideTask, requestIn, channel)
            requestOut.url = transformRequestUrl(backSideTask, requestIn, channel)

            // send request to backend service
            requestOut.run()

            // log response from backend service
            backSideTask.select()
            backSideTask.event.putResponseHeader(requestOut.responseHeaders)
            logResponseBody(backSideTask, requestOut)
            log.info "==> ${requestOut.status} ${(requestOut.response) ? requestOut.responseContentType : 'NULL'}"

            // transform backend service response for client
            HttpGeneralDetails responseOut = transformResponse(event.store.selectTask(Task.CLIENT_TASK), requestOut, channel)
            clientTask.select()

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
        String uri = req.requestURI //.toLowerCase()
        log.info "doGet ${uri}"
        SimStore simStore = parseUri(uri, req, resp, Verb.GET)
        if (!simStore)
            return

        String channelType = simStore.config.channelType
        assert channelType : "Sim ${simStore.simId} does not define a Channel Type."
        BaseChannel channel = (BaseChannel) proxyMap.get(channelType)
        assert channel : "Cannot create Channel of type ${channelType}"

        HttpGet requestIn = new HttpGet()

        Event event = simStore.newEvent()
        Task clientTask = event.store.selectClientTask()

        // log input request from client
        logRequestIn(event, requestIn, req, Verb.GET)

        // key request headers
        // accept-encoding: gzip
        // accept: *

        log.info "=> ${simStore.endpoint} ${event.store.requestHeader.accept}"

        // create new event task to manage interaction with service behind proxy
        Task backSideTask = event.store.newTask()

        // transform input request for backend service
        HttpGeneralDetails requestOut = transformRequest(backSideTask, requestIn, channel)
        requestOut.url = transformRequestUrl(backSideTask, requestIn, channel)

        // send request to backend service
        requestOut.run()

        // log response from backend service
        backSideTask.select()
        backSideTask.event.putResponseHeader(requestOut.responseHeaders)
        // TODO make this next line not seem to work
        //backSideTask.event._responseHeaders = requestOut._responseHeaders
        logResponseBody(backSideTask, requestOut)
        log.info "==> ${requestOut.status} ${(requestOut.response) ? requestOut.responseContentType : 'NULL'}"

        // transform backend service response for client
        clientTask.select()
        HttpGeneralDetails responseOut = transformResponse(event.store.selectTask(Task.CLIENT_TASK), requestOut, channel)

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

static HttpGeneralDetails logRequestIn(Event event, HttpGeneralDetails http, HttpServletRequest req, Verb verb) {
    event.store.selectRequest()

    // Log Headers
    RawHeaders rawHeaders = new RawHeaders()
    rawHeaders.addNames(req.headerNames)
    rawHeaders.names.each { String name ->
        rawHeaders.addHeaders(name, req.getHeaders(name))
    }
    rawHeaders.uriLine = "${verb} ${req.pathInfo}"
    Headers headers = HeaderBuilder.parseHeaders(rawHeaders)

    event.store.putRequestHeader(headers)
    http.requestHeaders = headers

    // Log body of POST
    if (verb == Verb.POST) {
        logRequestBody(event, headers, http, req)
    }

    http
}

static logRequestBody(Event event, Headers headers, HttpGeneralDetails http, HttpServletRequest req) {
    byte[] bytes = req.inputStream.bytes
    event.store.putRequestBody(bytes)
    http.request = bytes
    String encoding = headers.getContentEncoding()
    if (encoding == 'gzip') {
        String txt = Gzip.unzipWithoutBase64(bytes)
        event.store.putRequestBodyText(txt)
        http.requestText = txt
    } else if (headers.contentType == 'text/html') {
        event.store.putRequestHTMLBody(bytes)
        http.requestText = new String(bytes)
    } else {
        http.requestText = new String(bytes)
    }
}

static logResponseBody(Task task, HttpGeneralDetails http) {
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


static HttpGeneralDetails transformRequest(Task task, HttpPost requestIn, BaseChannel channelTransform) {
    HttpPost requestOut = new HttpPost()

    channelTransform.transformRequest(requestIn, requestOut)

    task.select()
    task.event.putRequestHeader(requestOut.requestHeaders)
    task.event.putRequestBody(requestOut.request)

    requestOut
}

static HttpGeneralDetails transformRequest(Task task, HttpGet requestIn, BaseChannel channelTransform) {
    HttpGet requestOut = new HttpGet()

    channelTransform.transformRequest(requestIn, requestOut)

    task.select()
    task.event.putRequestHeader(requestOut.requestHeaders)

    requestOut
}

static String transformRequestUrl(Task task, HttpGeneralDetails requestIn, BaseChannel channelTransform) {

    channelTransform.transformRequestUrl(task.event.simStore.endpoint, requestIn)

}

static HttpGeneralDetails transformResponse(Task task, HttpGeneralDetails responseIn, BaseChannel channelTransform) {
    HttpGeneralDetails responseOut = new HttpGet()  // here GET vs POST does not matter

    channelTransform.transformResponse(responseIn, responseOut)

    task.select()
    task.event.putResponseHeader(responseIn.responseHeaders)
    task.event.putResponseBody(responseIn.response)
    task.event.putResponseHeader(responseOut.responseHeaders)
    logResponseBody(task, responseOut)

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

    // the request targets a Channel

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
