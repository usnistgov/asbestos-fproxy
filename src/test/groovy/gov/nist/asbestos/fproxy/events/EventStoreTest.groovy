package gov.nist.asbestos.fproxy.events

import gov.nist.asbestos.fproxy.events.Event
import gov.nist.asbestos.fproxy.log.SimStore
import gov.nist.asbestos.simapi.http.headers.HeaderBuilder
import gov.nist.asbestos.simapi.tk.simCommon.SimId
import gov.nist.asbestos.simapi.tk.simCommon.TestSession
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class EventStoreTest extends Specification {

    @Rule
    TemporaryFolder tmp = new TemporaryFolder()

    def 'request response tasks' () {
        setup:
//        File ecHome = new File('/home/bill/tmp')
        File ecHome = tmp.newFolder('echome')
        File ec = new File(ecHome, 'ec')
        ec.mkdirs()

        TestSession testSession = new TestSession('default')
        SimId simId = new SimId(testSession, 'foo', 'reg', 'cat')
        SimStore simStore = new SimStore(ec, simId).withResource('store')
        simStore.getStore(true) // create event
        Event event = simStore.newEvent()

        String requestHdr = 'Request Header' + body()
        String requestBody = 'Request Body' + body()
        String task1ReqHdr = 'Task /1/Req/Header' + body()
        String task1ReqBody = 'Task /1/Req/Body' + body()
        String task1ResHdr = 'Task /1/Res/Header' + body()
        String task1ResBody = 'Task /1/Res/Body' + body()
        String task2ReqHdr = 'Task /2/Req/Header' + body()
        String task2ReqBody = 'Task /2/Req/Body' + body()
        String task2ResHdr = 'Task /2/Res/Header' + body()
        String task2ResBody = 'Task /2/Res/Body' + body()
        String responseHdr = 'Response Header' + body()
        String responseBody = 'Response Body' + body()

        /////////////////////////////////////////////////////
        when:  // build request with 2 tasks
        event.store.selectRequest()
        event.store.putRequestHeader(HeaderBuilder.rawHeadersFromString(requestHdr))
        event.store.putRequestBody(requestBody.bytes)

        event.store.newTask()
        event.store.putRequestHeader(HeaderBuilder.rawHeadersFromString(task1ReqHdr))
        event.store.putRequestBody(task1ReqBody.bytes)
        event.store.putResponseHeader(HeaderBuilder.parseHeaders(task1ResHdr))
        event.store.putResponseBody(task1ResBody.bytes)

        event.store.newTask()
        event.store.putRequestHeader(HeaderBuilder.rawHeadersFromString(task2ReqHdr))
        event.store.putRequestBody(task2ReqBody.bytes)
        event.store.putResponseHeader(HeaderBuilder.parseHeaders(task2ResHdr))
        event.store.putResponseBody(task2ResBody.bytes)

        event.store.selectRequest()
        event.store.putResponseHeader(HeaderBuilder.parseHeaders(responseHdr))
        event.store.putResponseBody(responseBody.bytes)

        then:
        event.store.selectTask(0)
        task1ReqHdr == event.store.getRequestHeader().toString()
        task1ReqBody == event.store.getRequestBodyAsString()
        task1ResHdr == event.store.getResponseHeader()
        task1ResBody == event.store.getResponseBodyAsString()

        event.store.selectRequest()
        requestHdr == event.store.getRequestHeader().toString()
        requestBody == event.store.getRequestBodyAsString()

        event.store.selectTask(1)
        task2ReqHdr == event.store.getRequestHeader().toString()
        task2ReqBody == event.store.getRequestBodyAsString()
        task2ResHdr == event.store.getResponseHeader()
        task2ResBody == event.store.getResponseBodyAsString()

        event.store.selectRequest()
        responseHdr == event.store.getResponseHeader()
        responseBody == event.store.getResponseBodyAsString()
    }

    def body() {
        '''\r
content-type: application/json\r
user-agent: Java/1.8.0_191\r
host: localhost:8080\r
accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r
connection: keep-alive\r
content-length: 31\r\n'''
    }
}
