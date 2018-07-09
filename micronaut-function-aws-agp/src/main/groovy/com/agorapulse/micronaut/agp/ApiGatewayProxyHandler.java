package com.agorapulse.micronaut.agp;

import com.agorapulse.micronaut.http.basic.BasicRequestHandler;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiGatewayProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    /**
     * This is the name of the environment which is set if the application in executed by this handler.
     */
    public static final String API_GATEWAY_PROXY_ENVIRONMENT = "api_gateway_proxy";

    @Override
    public final APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ApplicationContext applicationContext = buildApplicationContext(context);
        startEnvironment(applicationContext);
        applicationContext.registerSingleton(input);

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = Optional.ofNullable(input.getRequestContext()).orElseGet(APIGatewayProxyRequestEvent.ProxyRequestContext::new);
        APIGatewayProxyRequestEvent.RequestIdentity requestIdentity = Optional.ofNullable(requestContext.getIdentity()).orElseGet(APIGatewayProxyRequestEvent.RequestIdentity::new);

        applicationContext.registerSingleton(requestContext);
        applicationContext.registerSingleton(requestIdentity);

        doWithApplicationContext(applicationContext);

        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        HttpRequest request = convertToMicronautHttpRequest(input, applicationContext.getConversionService(), objectMapper);
        BasicRequestHandler inBoundHandler = applicationContext.getBean(BasicRequestHandler.class);
        HttpResponse response = inBoundHandler.handleRequest(request);
        return convertToApiGatewayProxyResponse(response);
    }

    protected void doWithApplicationContext(ApplicationContext applicationContext) {
        // an entry point to customize application context, useful for tests
    }

    private HttpRequest convertToMicronautHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService, ObjectMapper mapper) {
        return ApiGatewayProxyHttpRequest.create(input, conversionService, mapper);
    }

    private APIGatewayProxyResponseEvent convertToApiGatewayProxyResponse(HttpResponse response) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent().withStatusCode(response.status().getCode());

        if (response.body() instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) response.body();
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            responseEvent.setBody(new String(bytes));
        } else if (response.body() != null) {
            throw new IllegalStateException("Body is not ByteBuf: " + response.body());
        }

        responseEvent.setHeaders(response
            .getHeaders()
            .asMap()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)))
        );

        return responseEvent;
    }

    private ApplicationContext buildApplicationContext(Context context) {
        ApplicationContext applicationContext = ApplicationContext.build().environments(API_GATEWAY_PROXY_ENVIRONMENT).build();
        if (context != null) {
            registerContextBeans(context, applicationContext);
        }
        return applicationContext;
    }

    /**
     * Register the beans in the application.
     *
     * @param context context
     * @param applicationContext application context
     */
    private static void registerContextBeans(Context context, ApplicationContext applicationContext) {
        applicationContext.registerSingleton(context);
        LambdaLogger logger = context.getLogger();
        if (logger != null) {
            applicationContext.registerSingleton(logger);
        }
        ClientContext clientContext = context.getClientContext();
        if (clientContext != null) {
            applicationContext.registerSingleton(clientContext);
        }
        CognitoIdentity identity = context.getIdentity();
        if (identity != null) {
            applicationContext.registerSingleton(identity);
        }
    }

    /**
     * Start the environment specified.
     * @param applicationContext the application context with the environment
     * @return The environment within the context
     */
    private Environment startEnvironment(ApplicationContext applicationContext) {
        if (this instanceof PropertySource) {
            applicationContext.getEnvironment().addPropertySource((PropertySource) this);
        }

        return applicationContext
                .start()
                .getEnvironment();
    }
}
