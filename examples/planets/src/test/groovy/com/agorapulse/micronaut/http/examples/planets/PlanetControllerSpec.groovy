package com.agorapulse.micronaut.http.examples.planets

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import com.agorapulse.micronaut.agp.ApiGatewayProxyHandler
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import io.micronaut.context.ApplicationContext
import org.junit.Rule
import spock.lang.Specification

/**
 * Test for planet controller.
 */
class PlanetControllerSpec extends Specification {

    @Rule private final Gru gru = Gru.equip(ApiGatewayProxy.steal(this) {
        map '/planet/{star}' to ApiGatewayProxyHandler
        map '/planet/{star}/{name}' to ApiGatewayProxyHandler
    })

    @Rule private final Dru dru = Dru.steal(this)

    private final AmazonDynamoDB amazonDynamoDB = Mock(AmazonDynamoDB)

    @SuppressWarnings('UnusedPrivateField')
    private final ApiGatewayProxyHandler handler = new ApiGatewayProxyHandler() {
        @Override
        protected void doWithApplicationContext(ApplicationContext applicationContext) {
            applicationContext.registerSingleton(PlanetDBService, new PlanetDBService(amazonDynamoDB, DynamoDB.createMapper(dru)))
        }
    }

    void setup() {
        dru.add(new Planet(star: 'sun', name: 'mercury'))
        dru.add(new Planet(star: 'sun', name: 'venus'))
        dru.add(new Planet(star: 'sun', name: 'earth'))
        dru.add(new Planet(star: 'sun', name: 'mars'))
    }

    void 'get planet'() {
        expect:
            gru.test {
                get('/planet/sun/earth')
                expect {
                    json 'earth.json'
                }
            }
    }

    void 'get planet which does not exist'() {
        expect:
            gru.test {
                get('/planet/sun/vulcan')
                expect {
                    status NOT_FOUND
                }
            }
    }

    void 'list planets by existing star'() {
        expect:
            gru.test {
                get('/planet/sun')
                expect {
                    json 'planetsOfSun.json'
                }
            }
    }

    void 'add planet'() {
        when:
            gru.test {
                post '/planet/sun/jupiter'
                expect {
                    status CREATED
                    json 'jupiter.json'
                }
            }
        then:
            gru.verify()
            dru.findAllByType(Planet).size() == 5
    }

    void 'delete planet'() {
        given:
            dru.add(new Planet(star: 'sun', name: 'pluto'))
        expect:
            dru.findAllByType(Planet).size() == 5
            gru.test {
                delete '/planet/sun/pluto'
                expect {
                    status NO_CONTENT
                    json 'pluto.json'
                }
            }
            dru.findAllByType(Planet).size() == 4
    }

}
