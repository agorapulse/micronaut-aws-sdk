/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.http.examples.planets

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import io.micronaut.context.ApplicationContextBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Test for planet controller.
 */
class PlanetControllerSpec extends Specification {

    @AutoCleanup private final Gru gru = Gru.create(ApiGatewayProxy.steal(this) {      // <1>
        map '/planet/{star}' to MicronautHandler                                        // <2>
        map '/planet/{star}/{name}' to MicronautHandler
    })

    @AutoCleanup private final Dru dru = Dru.steal(this)

    void setup() {
        MicronautHandler.reset { ApplicationContextBuilder builder ->                   // <3>
            builder.singletons(DynamoDB.createMapper(dru))                              // <4>
        }
        dru.add(new Planet(star: 'sun', name: 'mercury'))
        dru.add(new Planet(star: 'sun', name: 'venus'))
        dru.add(new Planet(star: 'sun', name: 'earth'))
        dru.add(new Planet(star: 'sun', name: 'mars'))
    }

    void 'get planet'() {                                                               // <5>
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
