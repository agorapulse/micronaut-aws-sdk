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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiGatewayProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(BasicRequestHandler.class);

    /**
     * This is the name of the environment which is set if the application in executed by this handler.
     */
    public static final String API_GATEWAY_PROXY_ENVIRONMENT = "api_gateway_proxy";

    private final ApplicationContext context;

    public ApiGatewayProxyHandler() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting application context initialization");
        }
        this.context = buildApplicationContext();
        startEnvironment(context);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Application context initialization finished");
        }
    }

    // tag::handler[]
    @Override
    public final APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context lambdaContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting registration of custom request beans");
        }
        registerContextBeans(lambdaContext);

        context.registerSingleton(input);

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext = Optional.ofNullable(input.getRequestContext()).orElseGet(APIGatewayProxyRequestEvent.ProxyRequestContext::new);
        APIGatewayProxyRequestEvent.RequestIdentity requestIdentity = Optional.ofNullable(requestContext.getIdentity()).orElseGet(APIGatewayProxyRequestEvent.RequestIdentity::new);

        context.registerSingleton(requestContext);
        context.registerSingleton(requestIdentity);

        doWithApplicationContext(context);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Registration of custom request beans finished");
        }

        ObjectMapper mapper = context.getBean(ObjectMapper.class);
        ConversionService<?> conversionService = context.getConversionService();
        HttpRequest request = convertToMicronautHttpRequest(input, conversionService, mapper);  // <1>
        BasicRequestHandler inBoundHandler = context.getBean(BasicRequestHandler.class);        // <2>
        HttpResponse response = inBoundHandler.handleRequest(request);                          // <3>
        return convertToApiGatewayProxyResponse(response);                                      // <4>
    }
    // end::handler[]

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

    private ApplicationContext buildApplicationContext() {
        return ApplicationContext.build().environments(
            API_GATEWAY_PROXY_ENVIRONMENT,
            Environment.AMAZON_EC2,
            Environment.FUNCTION,
            Environment.CLOUD
        ).build();
    }

    /**
     * Register the beans in the application.
     *
     * @param lambdaContext context
     */
    private void registerContextBeans(Context lambdaContext) {
        context.registerSingleton(lambdaContext);
        LambdaLogger logger = lambdaContext.getLogger();
        if (logger != null) {
            context.registerSingleton(logger);
        }
        ClientContext clientContext = lambdaContext.getClientContext();
        if (clientContext != null) {
            context.registerSingleton(clientContext);
        }
        CognitoIdentity identity = lambdaContext.getIdentity();
        if (identity != null) {
            context.registerSingleton(identity);
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
