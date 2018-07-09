package com.agorapulse.micronaut.agp;

import com.agorapulse.micronaut.http.basic.BasicHttpHeaders;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.ConvertibleMultiValues;
import io.micronaut.core.convert.value.ConvertibleMultiValuesMap;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.*;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
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

    private static class ApiGatewayProxyHttpParameters implements HttpParameters {

        private final ConvertibleMultiValues<String> values;

        /**
         * @param parameters        The parameters
         * @param conversionService The conversion service
         */
        ApiGatewayProxyHttpParameters(Map<String, String> parameters, ConversionService conversionService) {
            LinkedHashMap<CharSequence, List<String>> values = new LinkedHashMap<>(parameters.size());
            this.values = new ConvertibleMultiValuesMap<>(values, conversionService);
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                values.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
        }

        @Override
        public Set<String> names() {
            return values.names();
        }

        @Override
        public Collection<List<String>> values() {
            return values.values();
        }

        @Override
        public List<String> getAll(CharSequence name) {
            return values.getAll(name);
        }

        @Override
        public String get(CharSequence name) {
            return values.get(name);
        }

        @Override
        public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
            return values.get(name, conversionContext);
        }
    }

    final APIGatewayProxyRequestEvent input;

    private final HttpParameters parameters;
    private final HttpMethod method;
    private final URI uri;
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();
    private final HttpHeaders headers;
    private final String serverName;
    private final String path;
    private final InetSocketAddress serverAddress;
    private final InetSocketAddress remoteAddress;

    ApiGatewayProxyHttpRequest(APIGatewayProxyRequestEvent input, ConversionService conversionService) {
        this.input = input;
        this.path = input.getPath();

        Map<String, String> queryStringParameters = Optional.ofNullable(input.getQueryStringParameters()).orElseGet(Collections::emptyMap);
        Map<String, String> httpHeaders = Optional.ofNullable(input.getHeaders()).orElseGet(Collections::emptyMap);
        String httpMethod = Optional.ofNullable(input.getHttpMethod()).orElse(DEFAULT_HTTP_METHOD);

        this.parameters = new ApiGatewayProxyHttpParameters(queryStringParameters, conversionService);
        this.headers = new BasicHttpHeaders(httpHeaders, conversionService);
        this.method = HttpMethod.valueOf(httpMethod.toUpperCase());

        try {
            // TODO: include query parameters
            this.uri = new URI(input.getPath());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Path is not valid URI:", e);
        }

        String region = Optional.ofNullable(System.getenv(AWS_REGION_ENV_NAME)).orElseGet(() -> LOCAL_DEVELOPMENT_AWS_REGION);

        APIGatewayProxyRequestEvent.ProxyRequestContext context = Optional.ofNullable(input.getRequestContext()).orElseGet(() ->
            new APIGatewayProxyRequestEvent.ProxyRequestContext()
                .withApiId(LOCAL_DEVELOPMENT_APP_ID)
        );

        serverName = context.getApiId() + ".execute-api." + region + ".amazonaws.com";
        serverAddress = new InetSocketAddress(serverName, HTTPS_PORT);

        APIGatewayProxyRequestEvent.RequestIdentity identity = Optional.ofNullable(context.getIdentity()).orElseGet(() ->
            new APIGatewayProxyRequestEvent.RequestIdentity().withSourceIp(LOCAL_DEVELOPMENT_SOURCE_IP)
        );

        remoteAddress = new InetSocketAddress(identity.getSourceIp(), HTTPS_PORT);
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
    public String getPath() {
        return path;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        return attributes;
    }
}
