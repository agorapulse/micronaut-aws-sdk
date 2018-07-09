package com.agorapulse.micronaut.agp

import groovy.transform.CompileStatic
import org.junit.runner.RunWith
import org.junit.runners.Suite

@CompileStatic
@RunWith(Suite)
@Suite.SuiteClasses([
    ApiGatewayProxyHttpRequestSpec,
    NettyHttpServerSpec
])
class ApiGatewayProxyTestSuite { }
