package com.agorapulse.micronaut.agp

import com.agorapulse.gru.Gru
import com.agorapulse.gru.agp.ApiGatewayProxy
import org.junit.Rule


class ApiGatewayProxyHttpRequestSpec extends AbstractApiGatewayProxyHttpRequestSpec {


    @Rule Gru gru = Gru.equip(ApiGatewayProxy.steal(this) {
        map '/hello' to ApiGatewayProxyHandler
        map '/hello/greet' to ApiGatewayProxyHandler
        map '/hello/greet/{message}/{language}' to ApiGatewayProxyHandler
        map '/hello/mfa' to ApiGatewayProxyHandler
    })

}
