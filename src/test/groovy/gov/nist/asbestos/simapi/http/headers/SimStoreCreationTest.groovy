package gov.nist.asbestos.simapi.http.headers

import gov.nist.asbestos.fproxy.channel.ChannelConfig
import gov.nist.asbestos.fproxy.channel.SimConfigMapper
import gov.nist.asbestos.fproxy.events.Event
import gov.nist.asbestos.fproxy.log.SimStore
import gov.nist.asbestos.fproxy.log.SimStoreBuilder
import gov.nist.asbestos.simapi.tk.simCommon.SimId
import gov.nist.asbestos.simapi.tk.simCommon.TestSession
import groovy.json.JsonSlurper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class SimStoreCreationTest extends Specification {

    @Rule
    TemporaryFolder tmp = new TemporaryFolder()

    def 'SimStore initialization' () {
        setup:
        println 'SimStore initialization'
        File ecHome = tmp.newFolder('echome')
//        File ecHome = new File('/home/bill/tmp')
        File ec = new File(ecHome, 'ec')

        TestSession testSession = new TestSession('default')

        /////////////////////////////////////////////////////
        when:  // under-specified SimId
        SimId simId = new SimId(testSession, 'foo')
        SimStore simStore = new SimStore(ec, simId)

        then:
        AssertionError e = thrown()
        e.message.contains('No actorType')
        e.message.contains('No environmentName')

        /////////////////////////////////////////////////////
        when:  // good SimId
        simId = new SimId(testSession, 'foo', 'reg', 'cat')
        simStore = new SimStore(ec, simId)

        then:
        true // no exception
        !ec.exists()  // ec access not needed yet so not checked

        /////////////////////////////////////////////////////
        when:  // ec directory does not exist - this must error
        // ec must be created manually
        simStore.getStore()

        then:
        e = thrown()
        e.message.contains('External Cache must exist')

        /////////////////////////////////////////////////////
        when: // manually create ec - now SimStore can initialize
        ec.mkdirs()
        simStore = new SimStore(ec, simId)
        simStore.getStore(true)  // this uses ec

        then: // fsimdb initialized
        new File(new File(ec, SimStore.PSIMDB), 'default').exists()
    }

    def 'from json config' () {
        def jsonString = '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon"
}
'''

        setup:
        println 'from json config'
        Map rawConfig = (Map) new JsonSlurper().parseText(jsonString)
        File ecHome = tmp.newFolder('echome')
//        File ecHome = new File('/home/bill/tmp')
        File ec = new File(ecHome, 'ec')
        ec.mkdirs()

        TestSession testSession = new TestSession('default')

        /////////////////////////////////////////////////////
        when:
        SimStore store = SimStoreBuilder.builder(ec, new ChannelConfig(rawConfig))

        then:
        store.channelId == new SimId(testSession, '1', 'baloon', 'default')
        ec.exists()
        File db = new File(ec, 'psimdb')
        db.exists()
        File defu = new File(db, 'default')
        defu.exists()
        File one = new File(defu, '1')
        one.exists()
        File config = new File(one, 'config.json')
        config.exists()

        when:
        SimStore store2 = SimStoreBuilder.loader(ec, new TestSession('default'), '1')

        then:
        store2.channelId == store.channelId

    }

    def 'event initialization' () {
        setup:
        println 'event initialization'
//        File ecHome = new File('/home/bill/tmp')
        File ecHome = tmp.newFolder('echome2')
        File ec = new File(ecHome, 'ec')
        ec.mkdirs()

        TestSession testSession = new TestSession('default')

        /////////////////////////////////////////////////////
        when:
        println '  starting when'
        SimId simId = new SimId(testSession, 'foo', 'reg', 'cat')
        SimStore simStore = new SimStore(ec, simId).withResource('store')
        simStore.getStore(true)  // create proxy
        println '    got simStore'
        Event event1 = simStore.newEvent()
        println '    new event'
        Event event2 = simStore.newEvent()
        println '    new event'

        then:
        println '  starting then'
        event1.store.root.exists()
        event2.store.root.exists()
        event1 != event2
    }

    def 'loader extensions' () {
        setup:
        println 'load extensions'
//        File ecHome = new File('/home/bill/tmp')
        File ecHome = tmp.newFolder('echome2')
        File ec = new File(ecHome, 'ec')
        ec.mkdirs()
        def jsonString = '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon",
  "extra1": "value1",
  "extra2": "value2"
}
'''

        TestSession testSession = new TestSession('default')

        /////////////////////////////////////////////////////
        when:
        Map jsonMap = (Map) new JsonSlurper().parseText(jsonString)
        ChannelConfig config = new SimConfigMapper(jsonMap).build()

        then:
        config.environment == 'default'
        config.testSession == 'default'
        config.channelId == '1'
        config.actorType == 'balloon'
        config.extensions.extra1 == 'value1'
        config.extensions.extra2 == 'value2'

    }

    def 'array in extensions' () {
        setup:
        println 'array in extensions'
//        File ecHome = new File('/home/bill/tmp')
        File ecHome = tmp.newFolder('echome2')
        File ec = new File(ecHome, 'ec')
        ec.mkdirs()
        def jsonString = '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon",
  "transactions": {
    "WRITE": "base",
    "READ": "base"
  }
}
'''

        TestSession testSession = new TestSession('default')

        /////////////////////////////////////////////////////
        when:
        Map jsonMap = (Map) new JsonSlurper().parseText(jsonString)

        then:
        jsonMap
        jsonMap.transactions.READ == 'base'

        when:
        ChannelConfig config = new SimConfigMapper(jsonMap).build()

        then:
        config.extensions.transactions.READ == 'base'
    }
}
