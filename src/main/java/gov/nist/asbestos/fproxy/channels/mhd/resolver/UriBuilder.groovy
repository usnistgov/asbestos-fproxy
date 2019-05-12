package gov.nist.asbestos.fproxy.channels.mhd.resolver

import groovy.transform.TypeChecked

/**
 * this is necessary because URI(String) will not do encoding.  You need
 * the multi-parameter version to get that support
 */
@TypeChecked
class UriBuilder {

    static URI build(String ref) {
        try {
            if (ref.startsWith('#'))
                return new URI(null, '', ref)
            String[] parts = ref.split(':', 2)
            if (parts.size() == 1)
                return new URI(null, parts[0], null)
            String[] partx = parts[1].split('#')
            String scheme = parts[0]
            String ssp = partx[0]
            String[] party = ssp.split('#', 2)
            ssp = party[0]
            String fragment = null
            if (party.size() == 2)
                fragment = party[1]
            new URI(parts[0], parts[1], fragment)
        } catch (ArrayIndexOutOfBoundsException e) {
            throw e
        }
    }

}
