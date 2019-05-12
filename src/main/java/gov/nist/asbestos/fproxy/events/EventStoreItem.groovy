package gov.nist.asbestos.fproxy.events

import groovy.transform.TypeChecked


@TypeChecked
class EventStoreItem {
    String eventId
    String actor
    String resource
    String verb
    File file

    String asJson() {
        '''
{
  "eventId": "EVENTID",
  "actor": "ACTOR",
  "resource": "RESOURCE",
  "verb": "VERB"
}'''.replace('EVENTID', eventId)
                .replace('ACTOR', actor)
                .replace('RESOURCE', resource)
                .replace('VERB', verb)
    }
}
