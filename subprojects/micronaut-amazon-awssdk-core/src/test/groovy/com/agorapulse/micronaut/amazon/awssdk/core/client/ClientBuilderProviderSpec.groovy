package com.agorapulse.micronaut.amazon.awssdk.core.client

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class ClientBuilderProviderSpec extends Specification {

    @Inject ClientBuilderProvider provider

    void 'known providers found'() {
        expect:
            verifyAll(provider) {
                findHttpClientBuilder(ClientBuilderProvider.APACHE).present
                findHttpClientBuilder(ClientBuilderProvider.AWS_CRT).present
                findHttpClientBuilder(ClientBuilderProvider.URL_CONNECTION).present

                findAsyncHttpClientBuilder(ClientBuilderProvider.NETTY).present
                findAsyncHttpClientBuilder(ClientBuilderProvider.AWS_CRT).present
            }
    }

}
