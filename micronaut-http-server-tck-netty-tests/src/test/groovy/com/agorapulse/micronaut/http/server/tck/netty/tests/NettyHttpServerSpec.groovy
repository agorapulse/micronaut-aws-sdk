package com.agorapulse.micronaut.http.server.tck.netty.tests

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.micronaut.http.server.tck.AbstractApiGatewayProxyHttpRequestSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.Rule
import spock.lang.AutoCleanup
import spock.lang.Shared

/**
 * Test for basic http server using Netty implementation.
 */
class NettyHttpServerSpec extends AbstractApiGatewayProxyHttpRequestSpec {

    @Shared @AutoCleanup EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    @Rule Gru gru = Gru.equip(Http.steal(this))

    void setup() {
        String serverUrl = embeddedServer.URL
        gru.prepare {
            baseUri serverUrl
        }
    }

}
