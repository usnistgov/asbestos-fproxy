package gov.nist.asbestos.fproxy.support

import gov.nist.asbestos.simapi.sim.basic.SimConfig


class BasicChannel {

    void basicValidateConfig(SimConfig simConfig) {
        simConfig.extensions.with {
            ((String) getProperty('base')).with {
                assert startsWith('http') : "Base is not a URL"
                assert ((List) getProperty('transactions')).size() > 0 : "No transactions"
            }
        }
    }

    String getURL(SimConfig simConfig, String transaction) {
        assert simConfig : "BasicChannel:getURL: simConfig is null"
        assert transaction : "BasicChannel:getURL: transaction is null"
        simConfig.extensions.transactions.with {
            String value = (String) getProperty(transaction)
            if (value.startsWith('base'))
                value = value.replace('base', (String) simConfig.extensions.base)
            value
        }
    }
}