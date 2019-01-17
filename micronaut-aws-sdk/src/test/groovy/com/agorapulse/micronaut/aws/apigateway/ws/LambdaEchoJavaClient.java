package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.function.client.FunctionClient;
import io.reactivex.Single;
import org.testcontainers.shaded.javax.inject.Named;

@FunctionClient
interface LambdaEchoJavaClient {

    @Named("lambda-echo-java")
    Single<WebSocketResponse> lambdaEcho(WebSocketRequest event);

}
