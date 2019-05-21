package gov.nist.asbestos.fproxy.channels.mhd

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceCacheMgr
import org.hl7.fhir.r4.model.Patient
import spock.lang.Specification

class ResourceCacheMgrTest extends Specification {

    def 'memory cache' () {
        setup:
        Patient patient1 = new Patient()
        patient1.addIdentifier().setSystem("urn:system").setValue("12345")
        patient1.addName().setFamily("Smith").addGiven("John")

        Patient patient2 = new Patient()
        patient2.addIdentifier().setSystem("urn:system").setValue("12345")
        patient2.addName().setFamily("Smith").addGiven("John")

        Patient patient3 = new Patient()
        patient3.addIdentifier().setSystem("urn:system").setValue("12345")
        patient3.addName().setFamily("Smith").addGiven("John")

        ResourceCacheMgr cache = new ResourceCacheMgr(null)
        Ref ref1 = new Ref('http://example.com/fhir/Patient/1')
        Ref ref2 = new Ref('http://example.com/fhir/Patient/2')
        Ref ref3 = new Ref('http://example.com/fhir1/Patient/1')

        when:
        cache.add(ref1, patient1)
        cache.add(ref2, patient2)
        cache.add(ref3, patient3)

        then:
        cache.caches.keySet().size() == 2
        cache.caches[ref1.base].readResource(ref1).url == ref1
        cache.caches[ref1.base].readResource(ref1).resource == patient1
    }
}
