package gov.nist.asbestos.fproxy.passthrough

import gov.nist.asbestos.fproxy.support.BasicChannel
import gov.nist.asbestos.fproxy.support.Channel
import gov.nist.asbestos.simapi.sim.basic.EventStore
import gov.nist.asbestos.simapi.sim.basic.SimConfig

/*
{
  "environment": "default",
  "testSession": "default",
  "simId": "1",                          // assigned by controller
  "actorType": "fhir",                   // basic fhir server

  "base": "http://localhost:8080/fhir",  // HAPI installation
  "transactions" [
    "WRITE": "base",                     // use base URL
    "READ": "base"                       // use base URL
  ]
}
 */


/**
 *
 */
class PassthroughChannel extends BasicChannel implements Channel {
    SimConfig simConfig = null

    @Override
    void setup(SimConfig simConfig) {
        this.simConfig = simConfig
    }

    @Override
    void teardown() {

    }

    @Override
    void validateConfig(SimConfig simConfig) {
        basicValidateConfig(simConfig)
    }

    @Override
    void handle(EventStore event) {

    }
}
