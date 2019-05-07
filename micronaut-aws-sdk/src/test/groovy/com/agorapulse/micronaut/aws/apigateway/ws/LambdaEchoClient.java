package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.function.client.FunctionClient;
import io.reactivex.Single;

import javax.inject.Named;

@FunctionClient
interface LambdaEchoClient {

    @Named("lambda-echo")
    Single<WebSocketResponse> lambdaEcho(WebSocketRequest event);

}
