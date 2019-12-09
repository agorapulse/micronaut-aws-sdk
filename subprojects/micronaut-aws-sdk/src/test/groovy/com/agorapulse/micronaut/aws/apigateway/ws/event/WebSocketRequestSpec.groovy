package com.agorapulse.micronaut.aws.apigateway.ws.event

import spock.lang.Specification

/**
 * Sanity checks for web socket event.
 */
class WebSocketRequestSpec extends Specification {

    void 'test to string'() {
        expect:
            new WebSocketRequest().toString() == 'WebSocketRequest{requestContext=null, body=\'null\', isBase64Encoded=null}'
    }

}
