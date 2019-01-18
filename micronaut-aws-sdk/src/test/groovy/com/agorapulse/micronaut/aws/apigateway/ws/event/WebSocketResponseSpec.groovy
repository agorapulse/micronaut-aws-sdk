package com.agorapulse.micronaut.aws.apigateway.ws.event

import spock.lang.Specification

/**
 * Sanity checks for web socket event.
 */
class WebSocketResponseSpec extends Specification {

    void 'sanity check'() {
        expect:
            WebSocketResponse.OK == new WebSocketResponse(200)
            WebSocketResponse.OK.toString() == new WebSocketResponse(200).toString()
            WebSocketResponse.OK.hashCode() == new WebSocketResponse(200).hashCode()

            WebSocketResponse.OK.statusCode == 200
            WebSocketResponse.ERROR.statusCode == 500
    }

}
