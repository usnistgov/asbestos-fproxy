package gov.nist.asbestos.fproxy.channel

import org.apache.log4j.Logger

class SimConfigMapper {
    static Logger log = Logger.getLogger(SimConfigMapper)
    Map parms

    SimConfigMapper(Map parms) {
        this.parms = parms
    }

    ChannelConfig build() {
        ChannelConfig simConfig
        Map extra = [:]

        int maxExtra = 50
        while (maxExtra > 0) {
            try {
                simConfig = new ChannelConfig(parms)
                simConfig.extensions = extra
                return simConfig
            } catch (Throwable t) {
                def msg = t.message
                if (msg.startsWith('No such property:')) {
                    msg = msg.substring(msg.indexOf(':') + 1).trim()
                    int firstSpace = msg.indexOf(' ')
                    String propName = msg.substring(0, firstSpace).trim()
                        extra[propName] = parms[propName]
                        parms.remove(propName)
                        log.debug "Moving ${propName} to extenions"
                    maxExtra--
                    continue
                }
                throw t
            }
        }
        assert true : "SimConfigMapper: Cannot load ${parms}"
    }
}
