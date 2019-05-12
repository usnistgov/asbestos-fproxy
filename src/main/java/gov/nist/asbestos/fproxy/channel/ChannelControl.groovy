package gov.nist.asbestos.fproxy.channel

import gov.nist.asbestos.fproxy.events.EventStore
import groovy.transform.TypeChecked

/**
 * Defines a proxy channel.
 * Implemented as a simulator.
 */
@TypeChecked
interface ChannelControl {
    void setup(ChannelConfig simConfig)
    void teardown()
    void validateConfig(ChannelConfig simConfig)
    // throws Assert if error
    void handle(EventStore event)
}
