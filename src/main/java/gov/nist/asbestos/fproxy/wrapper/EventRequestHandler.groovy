package gov.nist.asbestos.fproxy.wrapper

import gov.nist.asbestos.fproxy.events.EventStoreItem
import gov.nist.asbestos.fproxy.events.EventStoreSearch
import gov.nist.asbestos.fproxy.log.SimStore
import groovy.transform.TypeChecked

@TypeChecked
class EventRequestHandler {

    static String eventRequest(SimStore simStore, List<String> uriParts, Map<String, List<String>> parameters) {
        int last = -1
        if (uriParts.isEmpty()) {
            // asking for /Event  ??? - all events??? - must be some restricting parameters
            if (parameters.containsKey('_last')) {   //}   hasProperty('_last')) {
                List<String> lasts = parameters.get('_last')
                last = Integer.parseInt(lasts[0])
            }
        }

        EventStoreSearch search  = new EventStoreSearch(simStore.externalCache, simStore.channelId)
        Map<String, EventStoreItem> items = search.loadAllEventsItems() // key is eventId
        List<String> eventIds = items.keySet().sort()
        eventIds = eventIds.reverse()
        if (last > -1) {
            eventIds = eventIds.take(last)
        }
        StringBuilder buf = new StringBuilder()
        boolean first = true
        buf.append('{ "events":[\n')
        eventIds.each { String eventId ->
            if (!first) buf.append(',')
            first = false
            buf.append(items[eventId].asJson())
        }
        buf.append('\n]}')
        buf.toString()
    }

}
