package gov.nist.asbestos.fproxy.channels.mhd.resolver

import ca.uhn.fhir.context.FhirContext
import gov.nist.toolkit.fhir.server.utility.UriBuilder
import gov.nist.toolkit.utilities.io.Io
import groovy.transform.TypeChecked
import org.apache.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource
/**
 * Local cache of FHIR resources
 */
@TypeChecked
class FileSystemResourceCache implements ResourceCache {
    private static final Logger logger = Logger.getLogger(FileSystemResourceCache.class)
    static FhirContext ctx = FhirContext.forDstu3()

    private File cacheDir
    URI baseUrl

    FileSystemResourceCache(File cacheDir) {
        this.cacheDir = cacheDir
        File propFile = new File(cacheDir, 'cache.properties')
        assert propFile.exists() : "${cacheDir}/cache.properties does not exist"
        Properties props = new Properties()
        propFile.withInputStream { InputStream is -> props.load(is) }
        String base = props.getProperty('baseUrl')
        baseUrl = UriBuilder.build(base)
        logger.info("New Resource cache: ${base}  --> ${cacheDir}")
    }

    IBaseResource readResource(URI url) {
        File file = cacheFile(url, 'xml')
        if (file.exists())
            return ctx.newXmlParser().parseResource(file.text)
        file = cacheFile(url, 'json')
        if (file.exists())
            return ctx.newJsonParser().parseResource(file.text)
        return null
    }

    private File cacheFile(URI relativeUrl, fileType) {
        assert ResourceMgr.isRelative(relativeUrl)
        def type = ResourceMgr.resourceTypeFromUrl(relativeUrl)
        def id = ResourceMgr.id(relativeUrl) + ((fileType) ? ".${fileType}" : '')
        return new File(new File(cacheDir, type), id)
    }
}
