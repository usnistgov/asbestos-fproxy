package gov.nist.asbestos.fproxy.log

import groovy.transform.TypeChecked

@TypeChecked
class SimLog {
    File root

    SimLog(File externalCache) {
        root = new File(externalCache, '')
    }
}
