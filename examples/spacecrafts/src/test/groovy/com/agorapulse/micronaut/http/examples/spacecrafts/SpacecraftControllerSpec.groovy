package com.agorapulse.micronaut.http.examples.spacecrafts

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import com.agorapulse.micronaut.agp.ApiGatewayProxyHandler
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import io.micronaut.context.ApplicationContext
import org.junit.Rule
import spock.lang.Specification

/**
 * Specification for spacecraft controller.
 */
class SpacecraftControllerSpec extends Specification {

    @Rule private final Gru gru = Gru.equip(ApiGatewayProxy.steal(this) {
        map '/spacecraft/{country}' to ApiGatewayProxyHandler
        map '/spacecraft/{country}/{name}' to ApiGatewayProxyHandler
    })

    @Rule private final Dru dru = Dru.steal(this)

    @SuppressWarnings('UnusedPrivateField')
    private final ApiGatewayProxyHandler handler = new ApiGatewayProxyHandler() {
        @Override
        protected void doWithApplicationContext(ApplicationContext ctx) {
            ctx.registerSingleton(IDynamoDBMapper, DynamoDB.createMapper(dru))
        }
    }

    void setup() {
        dru.add(new Spacecraft(country: 'russia', name: 'vostok'))
        dru.add(new Spacecraft(country: 'usa', name: 'dragon'))
    }

    void 'get spacecraft'() {
        expect:
            gru.test {
                get('/spacecraft/usa/dragon')
                expect {
                    json 'dragon.json'
                }
            }
    }

    void 'get spacecraft which does not exist'() {
        expect:
            gru.test {
                get('/spacecraft/usa/vostok')
                expect {
                    status NOT_FOUND
                }
            }
    }

    void 'list spacecrafts by existing country'() {
        expect:
            gru.test {
                get('/spacecraft/usa')
                expect {
                    json 'usSpacecrafts.json'
                }
            }
    }

    void 'add planet'() {
        expect:
            gru.test {
                post '/spacecraft/usa/atlantis'
                expect {
                    status CREATED
                    json 'atlantis.json'
                }
            }
            dru.findAllByType(Spacecraft).size() == 3
    }

    void 'delete planet'() {
        given:
            dru.add(new Spacecraft(country: 'usa', name: 'x-15'))
        expect:
            dru.findAllByType(Spacecraft).size() == 3
            gru.test {
                delete '/spacecraft/usa/x-15'
                expect {
                    status NO_CONTENT
                    json 'x15.json'
                }
            }
            dru.findAllByType(Spacecraft).size() == 2
    }

}
