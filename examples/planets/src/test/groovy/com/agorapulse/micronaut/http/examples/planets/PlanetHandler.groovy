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
    APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
        String star = req.pathParameters.star

        if (req.httpMethod == 'GET' && req.path == '/planet/{star}') {
            return list(star)
        }

        if (req.path != '/planet/{star}/{name}') {
            return notFound(req.path)
        }

        String name = req.pathParameters.name

        switch (req.httpMethod) {
            case 'DELETE':
                return delete(star, name)
            case 'GET':
                return show(star, name)
            case 'POST':
                return create(star, name)
        }

        return methodNotAllowed(req)
    }

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
