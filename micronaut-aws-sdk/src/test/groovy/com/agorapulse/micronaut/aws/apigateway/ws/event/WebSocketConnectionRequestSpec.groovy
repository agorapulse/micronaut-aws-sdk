package com.agorapulse.micronaut.aws.apigateway.ws.event

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

/**
 * Sanity checks for web socket event.
 */
class WebSocketConnectionRequestSpec extends Specification {

    private static final long CREATION_TIME = System.currentTimeMillis()
    private static final long REQUEST_TIME = CREATION_TIME + 1000

    void 'sanity check'() {
        given:
            WebSocketConnectionRequest fromBuilders = createRequestSetters()
            WebSocketConnectionRequest fromSetters = createRequestBuilders()
        expect:
            fromBuilders == fromSetters
            fromBuilders.hashCode() == fromSetters.hashCode()
            fromBuilders.toString() == fromSetters.toString()
    }

    void 'serialization check'() {
        given:
            ObjectMapper mapper = new ObjectMapper()
            WebSocketConnectionRequest request = createRequestBuilders()
        expect:
            request == mapper.readerFor(WebSocketConnectionRequest).readValue(mapper.writeValueAsString(request))
    }

    private static WebSocketConnectionRequest createRequestSetters() {
        return new WebSocketConnectionRequest(
            body: 'Hello',
            requestContext: new RequestContext(
                routeKey: '$connect',
                messageId: 'messageId',
                eventType: EventType.CONNECT,
                extendedRequestId: 'abcdefghijkl',
                messageDirection: MessageDirection.IN,
                stage: 'test',
                connectedAt: CREATION_TIME,
                requestTimeEpoch: REQUEST_TIME,
                identity: new Identity(sourceIp: '127.0.0.1'),
                requestId: 'abcdefg',
                domainName: 'localhost',
                connectionId: 'abcdefghij',
                apiId: 'appidabcd'
            ),
            headers: ['User-Agent': 'Spock'],
            multiValueHeaders: ['User-Agent': ['Spock']],
            isBase64Encoded: false
        )
    }

    private static WebSocketConnectionRequest createRequestBuilders() {
        return new WebSocketConnectionRequest()
            .withHeaders('User-Agent': 'Spock')
            .withMultiValueHeaders('User-Agent': ['Spock'])
            .withBody('Hello')
            .withRequestContext(new RequestContext()
                .withRouteKey('$connect')
                .withMessageId('messageId')
                .withEventType(EventType.CONNECT)
                .withExtendedRequestId('abcdefghijkl')
                .withMessageDirection(MessageDirection.IN)
                .withStage('test')
                .withConnectedAt(CREATION_TIME)
                .withRequestTimeEpoch(REQUEST_TIME)
                .withIdentity(new Identity().withSourceIp('127.0.0.1'))
                .withRequestId('abcdefg')
                .withDomainName('localhost')
                .withConnectionId('abcdefghij')
                .withApiId('appidabcd')
            )
            .withIsBase64Encoded(false) as WebSocketConnectionRequest
    }

}
