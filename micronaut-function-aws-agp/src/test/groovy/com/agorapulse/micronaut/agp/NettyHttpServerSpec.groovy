package com.agorapulse.micronaut.agp

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.Rule
import spock.lang.AutoCleanup
import spock.lang.Shared

class NettyHttpServerSpec extends AbstractApiGatewayProxyHttpRequestSpec {

    @Shared @AutoCleanup EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    @Rule Gru gru = Gru.equip(Http.steal(this))

    void setup() {
        String serverUrl = embeddedServer.getURL().toString()
        gru.prepare {
            baseUri serverUrl
        }
    }

}
