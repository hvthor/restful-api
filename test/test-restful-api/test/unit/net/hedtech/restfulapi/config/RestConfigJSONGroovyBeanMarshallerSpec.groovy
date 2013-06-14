/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.*
import grails.test.mixin.support.*
import net.hedtech.restfulapi.marshallers.json.*
import net.hedtech.restfulapi.beans.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigJSONGroovyBeanMarshallerSpec extends Specification {

    def "Test json groovy bean marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'groovyBean' marshallers {
                    jsonGroovyBeanMarshaller {
                        supports SimpleBean
                        field 'foo' name 'bar'
                        includesFields {
                            field 'bar' name 'customBar'
                        }
                        excludesFields {
                            field 'foobar'
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        def marshaller = config.marshallerGroups['groovyBean'].marshallers[0].instance

        then:
        SimpleBean                                == marshaller.supportClass
        ['foo':'bar','bar':'customBar']           == marshaller.fieldNames
        ['bar']                                   == marshaller.includedFields
        ['foobar']                                == marshaller.excludedFields
    }

    def "Test json groovy bean marshaller template parsing"() {
        setup:
        def src =
        {
            jsonGroovyBeanMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports SimpleBean
                    field 'foo' name 'bar'
                    field 'f1'
                    field 'f2'
                    includesFields {
                        field 'foo' name 'foobar'
                    }
                    excludesFields {
                        field 'bar'
                    }
                    additionalFields {->}
                    additionalFieldsMap = ['a':'b','c':'d']
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.groovyBean.configs['two']

        then:
         2                     == config.groovyBean.configs.size()
         ['one']               == mConfig.inherits
         5                     == mConfig.priority
         SimpleBean            == mConfig.supportClass
         ['foo':'foobar']      == mConfig.fieldNames
         ['foo']               == mConfig.includedFields
         ['bar']               == mConfig.excludedFields
         1                     == mConfig.additionalFieldClosures.size()
         ['a':'b','c':'d']     == mConfig.additionalFieldsMap
    }

    def "Test json groovy bean marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonGroovyBeanMarshaller {
                            supports SimpleBean
                            field 'owner' name 'myOwner'
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts'
                            }
                            excludesFields {
                                field 'description'
                            }
                            additionalFields {Map m ->}
                            additionalFieldsMap = ['foo':'bar']
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].instance

        then:
        SimpleBean                               == marshaller.supportClass
        ['owner':'myOwner','code':'productCode'] == marshaller.fieldNames
        ['code','parts']                         == marshaller.includedFields
        ['description']                          == marshaller.excludedFields
        1                                        == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                            == marshaller.additionalFieldsMap
    }

    def "Test json groovy bean marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            jsonGroovyBeanMarshallerTemplates {
                template 'one' config {
                    includesFields {
                        field 'field1'
                    }
                }

                template 'two' config {
                    includesFields {
                        field 'field2'
                    }
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonGroovyBeanMarshaller {
                            inherits = ['one','two']
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description'
                            }
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].instance

        then:
        ['field1','field2','code','description'] == marshaller.includedFields
    }

}