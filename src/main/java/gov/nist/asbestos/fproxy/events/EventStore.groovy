package gov.nist.asbestos.fproxy.events

import gov.nist.asbestos.fproxy.log.SimStore
import gov.nist.asbestos.fproxy.log.Task
import gov.nist.asbestos.simapi.http.headers.Headers
import gov.nist.asbestos.simapi.http.headers.HeaderBuilder
import gov.nist.asbestos.simapi.http.headers.RawHeaders
import groovy.transform.TypeChecked


/**
 * An EventStore is a request (the trigger) and any number of tasks undertaken
 * to satisfy that request.
 */
@TypeChecked
class EventStore {
    SimStore simStore
    File root
    File _request = null // interaction with client
    List<File> _tasks = [] // downstream/backend interactions
    File current = null // either request or a task

    Event e = null;

    def clearCache() {
        e = new Event()
    }

    Event newEvent() {
        e = new Event(this, simStore.channelId, simStore.resource, simStore.eventId)
        assert e.complete : "Trying to create new event without details."
        e
    }

    EventStore(SimStore simStore, File eventDir) {
        this.simStore = simStore
        this.root = eventDir
        int i = 0
        while (true) {
            File taskFile = getTaskFile(i)
            if (taskFile.exists())
                _tasks[i] = taskFile
            else
                break
            i++
        }
        clearCache()
    }

    /**
     * creates request (if doesn't exist) and sets it as current
     * @return the request dir
     */
    File getRequest() {
        if (!_request) {
            _request = new File(root, 'request')
            _request.mkdir()
        }
        current = _request
        clearCache()
        _request
    }

    File getTaskFile(int i) {
        new File(root, "task${i}")
    }

    /**
     * creates new task and sets it as current
     * @return the task dir
     */
    Task newTask() {
        int i = _tasks.size()
        File task = getTaskFile(i)
        task.mkdir()
        current = task
        _tasks << task
        clearCache()
        new Task(this, i)
    }

    int getTaskCount() {
        _tasks.size()
    }

    /**
     * Select a task as current
     * @param i selectTask(-1) is the same as selectRequest() except for the return type
     */
    Task selectTask(int i) {
        assert i < taskCount : "EventStore: cannot return task #${i} - only ${taskCount} tasks\n"
        if (i < 0)
            current = _request
        else
            current = _tasks[i]
        clearCache()
        new Task(this, i)
    }

    Task selectClientTask() {
        selectTask(-1)
    }

    /**
     * select request as current
     * @return
     */
    EventStore selectRequest() {
        if (!_request)
            getRequest()
        current = _request
        clearCache()
        this
    }


    private File getRequestHeaderFile() { new File(current, 'request_header.txt') }
    private File getRequestBodyFile() { new File(current, 'request_body.bin') }
    private File getRequestBodyStringFile() {  new File(current, 'request_body.txt') }
    private File getResponseHeaderFile() {  new File(current, 'response_header.txt') }
    private File getResponseBodyFile() {  new File(current, 'response_body.bin') }
    private File getResponseBodyStringFile() {  new File(current, 'response_body.txt') }
    private File getResponseBodyHTMLFile() {  new File(current, 'response_body.html') }
    private File getRequestBodyHTMLFile() {  new File(current, 'request_body.html') }

    void putRequestHeader(RawHeaders rawHeaders) {
        //e._requestRawHeaders = rawHeaders
        e._requestHeaders = HeaderBuilder.parseHeaders(rawHeaders)
        current.mkdirs()
        String txt = "${HeaderBuilder.headersAsString(rawHeaders)}"
        requestHeaderFile.text = txt
    }

    void putRequestHeader(Headers headers) {
        e._requestHeaders = headers
        current.mkdirs()
        requestHeaderFile.text = headers.toString()
    }

    void putRequestBody(byte[] body) {
        e._requestRawBody = body
        current.mkdirs();
        if (body.size()) {
            requestBodyFile.withOutputStream { it.write body }
            e._requestBody = new String(body)
            requestBodyStringFile.text = requestBody
        }
    }
    Headers getRequestHeader() {
        if (!e._requestHeaders) {
            String headerString = requestHeaderFile.text
            RawHeaders requestRawHeaders = HeaderBuilder.rawHeadersFromString(headerString)
            e._requestHeaders = HeaderBuilder.parseHeaders(requestRawHeaders)
        }
        e._requestHeaders
    }
    byte[] getRequestBody() {
        if (!e._requestRawBody) {
            e._requestRawBody = requestBodyFile.readBytes()
            e._requestBody = new String(e._requestRawBody)
        }
        e._requestRawBody
    }

    String getRequestBodyAsString() {
        getRequestBody()
        e._requestBody
    }

    void putResponseHeader(Headers headers) {
        e._responseHeaders = headers
        putResponseHeaderInternal(headers.toString())
    }

    private void putResponseHeaderInternal(String header) {
        current.mkdirs()
        File f = responseHeaderFile
        f.text = header
    }

    void putResponseBody(byte[] body) {
        current.mkdirs();
        responseBodyFile.bytes = body
        e._responseRawBody = body
    }

    void putResponseBodyText(String body) {
        responseBodyStringFile.text = body
    }

    void putRequestBodyText(String body) {
        requestBodyStringFile.text = body
    }

    void putResponseHTMLBody(byte[] body) {
        current.mkdirs();
        responseBodyFile.withOutputStream { it.write body }
        e._responseRawBody = body
        e._responseBody = new String(body)
        responseBodyStringFile.text = responseBody
        responseBodyHTMLFile.text = responseBody
    }

    void putRequestHTMLBody(byte[] body) {
        current.mkdirs();
        requestBodyFile.withOutputStream { it.write body }
        e._requestRawBody = body
        e._requestBody = new String(body)
        requestBodyStringFile.text = requestBody
        requestBodyHTMLFile.text = requestBody
    }

    String getResponseHeader() {
        if (!e._responseHeaders) {
            String headerString = responseHeaderFile.text
            e._responseRawHeaders = HeaderBuilder.rawHeadersFromString(headerString)
            e._responseHeaders = HeaderBuilder.parseHeaders(e._responseRawHeaders)
        }
        e._responseHeaders
    }

    byte[] getResponseBody() {
        if (!e._responseRawBody) {
            e._responseRawBody = responseBodyFile.readBytes()
            e._responseBody = new String(e._responseRawBody)
        }
        e._responseRawBody
    }

    String getResponseBodyAsString() {
        getResponseBody()
        e._responseBody
    }

}
