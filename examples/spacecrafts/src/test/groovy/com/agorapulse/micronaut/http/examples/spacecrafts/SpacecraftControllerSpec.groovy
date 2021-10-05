/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.http.examples.spacecrafts

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import io.micronaut.context.ApplicationContextBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Specification for spacecraft controller.
 */
class SpacecraftControllerSpec extends Specification {

    @AutoCleanup private final Gru gru = Gru.create(ApiGatewayProxy.steal(this) {
        map '/spacecraft/{country}' to MicronautHandler
        map '/spacecraft/{country}/{name}' to MicronautHandler
    })

    @AutoCleanup private final Dru dru = Dru.steal(this)

    void setup() {
        MicronautHandler.reset { ApplicationContextBuilder builder ->
            builder.singletons(DynamoDB.createMapper(dru))
        }
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
