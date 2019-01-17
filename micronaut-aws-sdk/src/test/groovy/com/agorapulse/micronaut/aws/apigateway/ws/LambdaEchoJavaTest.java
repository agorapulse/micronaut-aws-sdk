package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.EventType;
import com.agorapulse.micronaut.aws.apigateway.ws.event.RequestContext;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LambdaEchoJavaTest {

    private static final String CONNECTION_ID = "abcdefghij";
    private static final String BODY = "Hello";
    private static final String RESPONSE = "[abcdefghij] Hello";

    private ApplicationContext ctx;
    private EmbeddedServer server;

    private LambdaEchoJavaClient client;

    private MessageSender sender;
    private MessageSenderFactory factory;
    private TestTopicPublisher publisher;

    @Before
    public void setup() {
        sender = mock(MessageSender.class);
        publisher = mock(TestTopicPublisher.class);

        factory = (url) -> sender;

        ctx = ApplicationContext.build().build();

        ctx.registerSingleton(MessageSenderFactory.class, factory);
        ctx.registerSingleton(TestTopicPublisher.class, publisher);

        ctx.start();

        server = ctx.getBean(EmbeddedServer.class).start();

        client = ctx.createBean(LambdaEchoJavaClient.class, server.getURL());
    }

    @After
    public void tearDown() {
        server.close();
        ctx.close();
    }

    @Test
    public void testConnect() {
        WebSocketRequest request = new WebSocketRequest()
            .withRequestContext(
                new RequestContext().withEventType(EventType.CONNECT).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, never()).send(CONNECTION_ID, RESPONSE);
    }

    @Test
    public void testDisconnect() {
        WebSocketRequest request = new WebSocketRequest()
            .withRequestContext(
                new RequestContext().withEventType(EventType.DISCONNECT).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, never()).send(CONNECTION_ID, RESPONSE);
    }

    @Test
    public void testMessage() {
        WebSocketRequest request = new WebSocketRequest()
            .withBody(BODY)
            .withRequestContext(
                new RequestContext().withEventType(EventType.MESSAGE).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, times(1)).send(CONNECTION_ID, RESPONSE);
        verify(publisher, times(1)).publishMessage(CONNECTION_ID, RESPONSE);
    }

}
