package com.agorapulse.micronaut.http.examples.planets

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.sun.xml.internal.ws.server.UnsupportedMediaException
import groovy.transform.CompileStatic

@CompileStatic
class PlanetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // tag::badcode[]
    private final PlanetDBService planetDBService

    PlanetHandler() {
        this.planetDBService = new PlanetDBService()
    }

    @Override
    APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        if (input.httpMethod == 'GET' && input.path == '/planet/{star}') {
            return list(input.pathParameters.star)
        }

        if (input.path != '/planet/{star}/{name}') {
            return notFound(input.path)
        }

        switch (input.httpMethod) {
            case 'DELETE': return delete(input.pathParameters.star, input.pathParameters.name)
            case 'GET': return show(input.pathParameters.star, input.pathParameters.name)
            case 'POST': return create(input.pathParameters.star, input.pathParameters.name)
        }

        return methodNotAllowed(input)
    }
    // end::badcode[]

    APIGatewayProxyResponseEvent methodNotAllowed(APIGatewayProxyRequestEvent input) {
        throw new UnsupportedMediaException()
    }

    APIGatewayProxyResponseEvent create(String star, String name) {
        throw new UnsupportedMediaException()
    }

    APIGatewayProxyResponseEvent show(String star, String name) {
        throw new UnsupportedMediaException()
    }

    APIGatewayProxyResponseEvent delete(String star, String name) {
        throw new UnsupportedMediaException()
    }

    APIGatewayProxyResponseEvent notFound(String path) {
        throw new UnsupportedMediaException()
    }

    APIGatewayProxyResponseEvent list(String star) {
        throw new UnsupportedMediaException()
    }


}
