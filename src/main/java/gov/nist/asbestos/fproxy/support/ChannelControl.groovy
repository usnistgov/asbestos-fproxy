package gov.nist.asbestos.fproxy.support


import gov.nist.asbestos.simapi.sim.basic.EventStore
import gov.nist.asbestos.simapi.sim.basic.ChannelConfig
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
