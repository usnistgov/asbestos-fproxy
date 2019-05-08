package gov.nist.asbestos.fproxy.support


import gov.nist.asbestos.simapi.sim.basic.EventStore
import gov.nist.asbestos.simapi.sim.basic.SimConfig
import groovy.transform.TypeChecked

/**
 * Defines a proxy channel.
 * Implemented as a simulator.
 */
@TypeChecked
interface Channel {
    void setup(SimConfig simConfig)
    void teardown()
    void validateConfig(SimConfig simConfig)
    // throws Assert if error
    void handle(EventStore event)
}
