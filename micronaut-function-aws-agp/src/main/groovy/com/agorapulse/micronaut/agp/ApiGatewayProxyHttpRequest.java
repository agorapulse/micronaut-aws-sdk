package com.agorapulse.micronaut.agp;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.*;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.http.simple.SimpleHttpParameters;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

abstract class ApiGatewayProxyHttpRequest<B> implements HttpRequest<B> {

    private static final String LOCAL_DEVELOPMENT_APP_ID = "local";
    private static final String LOCAL_DEVELOPMENT_SOURCE_IP = "127.0.0.1";
    private static final String LOCAL_DEVELOPMENT_AWS_REGION = "eu-west-1";

    static HttpRequest create(APIGatewayProxyRequestEvent input, ConversionService conversionService, ObjectMapper mapper) {
        if (input.getIsBase64Encoded() == Boolean.TRUE) {
            return new BinaryApiGatewayProxyHttpRequest(input, conversionService);
        }

        boolean isJson = Optional
            .ofNullable(input.getHeaders())
            .flatMap(headers -> Optional.ofNullable(headers.get("Content-Type")))
            .map(contentType -> contentType.contains("json"))
            .orElse(false);

        if (isJson) {
            return new JsonApiGatewayProxyHttpRequest(input, conversionService, mapper);
        }

        return new TextApiGatewayProxyHttpRequest(input, conversionService);
    }

    private static final int HTTPS_PORT = 443;
    private static final String AWS_REGION_ENV_NAME = "AWS_REGION";
    private static final String DEFAULT_HTTP_METHOD = "GET";

    private static class BinaryApiGatewayProxyHttpRequest extends ApiGatewayProxyHttpRequest<byte[]> {
        BinaryApiGatewayProxyHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService) {
            super(input, conversionService);
        }

        @Override
        public Optional<byte[]> getBody() {
            return Optional.ofNullable(input.getBody()).map(encoded -> Base64.getDecoder().decode(encoded));
        }
    }

    private static class TextApiGatewayProxyHttpRequest extends ApiGatewayProxyHttpRequest<String> {
        TextApiGatewayProxyHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService) {
            super(input, conversionService);
        }

        @Override
        public Optional<String> getBody() {
            return Optional.ofNullable(input.getBody());
        }
    }

    /**
     * I don't know how any other way how to tell Micronaut that the body is JSON so I parse it for it
     * @author vlad
     */
    private static class JsonApiGatewayProxyHttpRequest extends ApiGatewayProxyHttpRequest<JsonNode> {

        private final ObjectMapper mapper;

        JsonApiGatewayProxyHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService, ObjectMapper mapper) {
            super(input, conversionService);
            this.mapper = mapper;
        }

        @Override
        public Optional<JsonNode> getBody() {
            return Optional.ofNullable(input.getBody()).map(body -> {
                try {
                    return mapper.reader().readTree(body);
                } catch (IOException e) {
                    throw new IllegalStateException("Unparsable body: " + input.getBody(), e);
                }
            });
        }
    }

    private static class EmptyCookies implements Cookies {
        static final Cookies INSTANCE = new EmptyCookies();

        @Override
        public Set<Cookie> getAll() {
            return Collections.emptySet();
        }

        @Override
        public Optional<Cookie> findCookie(CharSequence name) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
            return Optional.empty();
        }

        @Override
        public Collection<Cookie> values() {
            return Collections.emptySet();
        }
    }

    final APIGatewayProxyRequestEvent input;

    private final SimpleHttpParameters parameters;
    private final HttpMethod method;
    private final URI uri;
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();
    private final MutableHttpHeaders headers;
    private final InetSocketAddress remoteAddress;

    ApiGatewayProxyHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService) {
        this.input = input;

        String httpMethod = Optional.ofNullable(input.getHttpMethod()).orElse(DEFAULT_HTTP_METHOD);
        this.method = HttpMethod.valueOf(httpMethod.toUpperCase());

        APIGatewayProxyRequestEvent.ProxyRequestContext context = Optional.ofNullable(input.getRequestContext()).orElseGet(() ->
            new APIGatewayProxyRequestEvent.ProxyRequestContext()
                .withApiId(LOCAL_DEVELOPMENT_APP_ID)
        );

        String region = Optional.ofNullable(System.getenv(AWS_REGION_ENV_NAME)).orElseGet(() -> LOCAL_DEVELOPMENT_AWS_REGION);
        String serverName = context.getApiId() + ".execute-api." + region + ".amazonaws.com";

        APIGatewayProxyRequestEvent.RequestIdentity identity = Optional.ofNullable(context.getIdentity()).orElseGet(() ->
            new APIGatewayProxyRequestEvent.RequestIdentity().withSourceIp(LOCAL_DEVELOPMENT_SOURCE_IP)
        );

        remoteAddress = new InetSocketAddress(identity.getSourceIp(), HTTPS_PORT);

        MutableHttpRequest request = HttpRequest.create(this.method, "https://" + serverName + input.getPath());

        this.uri = request.getUri();

        Map<String, String> httpHeaders = Optional.ofNullable(input.getHeaders()).orElseGet(Collections::emptyMap);
        this.headers = request.getHeaders();
        httpHeaders.forEach(this.headers::add);

        Map<String, String> queryStringParameters = Optional.ofNullable(input.getQueryStringParameters()).orElseGet(Collections::emptyMap);
        this.parameters = new SimpleHttpParameters(conversionService);
        queryStringParameters.forEach(this.parameters::add);
    }

    @Override
    public Cookies getCookies() {
        return EmptyCookies.INSTANCE;
    }

    @Override
    public HttpParameters getParameters() {
        return parameters;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        return attributes;
    }
}
