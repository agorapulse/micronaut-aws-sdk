package com.agorapulse.micronaut.http.examples

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.micronaut.http.examples.spacecrafts.SpacecraftDBService
import com.amazonaws.services.lambda.runtime.Context
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.Rule
import spock.lang.Specification

/**
 * Sanity tests for local server.
 */
class LocalServerSpec extends Specification {

   @Rule Gru gru = Gru.equip(Http.steal(this))

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
