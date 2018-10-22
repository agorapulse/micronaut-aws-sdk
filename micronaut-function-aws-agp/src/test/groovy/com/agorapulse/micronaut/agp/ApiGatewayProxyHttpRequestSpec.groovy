package com.agorapulse.micronaut.agp

import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import com.agorapulse.micronaut.http.server.tck.AbstractApiGatewayProxyHttpRequestSpec
import org.junit.Rule
import spock.lang.Unroll


class ApiGatewayProxyHttpRequestSpec extends AbstractApiGatewayProxyHttpRequestSpec {


    @Rule Gru gru = Gru.equip(ApiGatewayProxy.steal(this) {
        map '/hello' to ApiGatewayProxyHandler
        map '/hello/greet' to ApiGatewayProxyHandler
        map '/hello/greet/{message}/{language}' to ApiGatewayProxyHandler
        map '/hello/mfa' to ApiGatewayProxyHandler
    })

    @Unroll
    void 'test reconstruct path #path with variable #variables to #original'() {
        expect:
            ApiGatewayProxyHttpRequest.reconstructPath(resource, variables) == original
        where:
            original    | resource       | variables
            '/foo/bar'  | '/foo/bar'     | null
            '/foo/bar'  | '/foo/{place}' | [place: 'bar']
            '/foo/bar'  | '/{proxy+}'    | [proxy: 'foo/bar']
    }

}
