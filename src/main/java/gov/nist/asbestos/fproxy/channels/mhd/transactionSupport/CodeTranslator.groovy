package gov.nist.asbestos.fproxy.channels.mhd.transactionSupport

import org.hl7.fhir.dstu3.model.CodeType


class CodeTranslator {
    def codeTypes = []

    CodeTranslator(String codesXmlFileContent) {
        parse(codesXmlFileContent)
    }

    def parse(stringXML) {
        def codes = new XmlSlurper().parseText(stringXML)

        codes.CodeType.each { codeTypeEle ->
            CodeType codeType = new CodeType(codeTypeEle)
            codeTypes << codeType
            codeTypeEle.Code.each { codeEle ->
                codeType.codes << new Code(codeEle)
            }
        }
    }

    Code findCodeBySystem(String systemCodeType, String system, String code) {
        codeTypes.find { it.name == systemCodeType }.codes.find { it.system == system && it.code == code }
    }

    Code findCodeByClassificationAndSystem(String classification, String system, String code) {
        def codeType = codeTypes.find { it.classScheme == classification }
        codeType.codes.find { it.system == system && it.code == code }
    }


    static final String CONFCODE = 'confidentialityCode'
    static final String HCFTCODE = 'healthcareFacilityTypeCode'
    static final String PRACCODE = 'practiceSettingCode'
    static final String EVENTCODE = 'eventCodeList'
    static final String FOLDERCODE = 'folderCodeList'
    static final String TYPECODE = 'typeCode'
    static final String CONTENTTYPECODE = 'contentTypeCode'
    static final String CLASSCODE = 'classCode'
    static final String FORMATCODE = 'formatCode'

    def systemCodeTypes = [
            CONFCODE,
            HCFTCODE,
            PRACCODE,
            EVENTCODE,
            FOLDERCODE,
            TYPECODE,
            CONTENTTYPECODE,
            CLASSCODE,
            FORMATCODE
    ]

}
