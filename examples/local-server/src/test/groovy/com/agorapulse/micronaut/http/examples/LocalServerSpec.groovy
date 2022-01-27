/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.http.examples

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.micronaut.http.examples.spacecrafts.SpacecraftDBService
import com.amazonaws.services.lambda.runtime.Context
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Sanity tests for local server.
 */
class LocalServerSpec extends Specification {

   @AutoCleanup Gru gru = Gru.equip(Http.steal(this))

    void 'sanity check'() {
        when:
            Application.main()
            EmbeddedServer server = Application.context.getBean(EmbeddedServer)
            gru.prepare { baseUri server.URL.toExternalForm() }
        then:
            Application.context.getBean(Context)
            Application.context.getBean(SpacecraftDBService)

            gru.test {
                post '/planet/sun/jupiter'
                expect {
                    status CREATED
                    json 'jupiter.json'
                }
            }
        cleanup:
            Application.context.stop()
    }

}
