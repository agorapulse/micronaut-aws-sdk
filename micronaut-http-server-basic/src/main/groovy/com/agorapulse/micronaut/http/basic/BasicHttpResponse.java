package com.agorapulse.micronaut.http.basic;

import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Optional;

class BasicHttpResponse<B> implements MutableHttpResponse<B> {

    private final MutableHttpHeaders headers = new BasicHttpHeaders(new LinkedHashMap<>(), ConversionService.SHARED);
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();

    private HttpStatus status = HttpStatus.OK;
    private B body;

    @Override
    public MutableHttpResponse<B> cookie(Cookie cookie) {
        throw new UnsupportedOperationException("Cookies are not supported in API Gateway Proxy");
    }

    @Override
    public MutableHttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        return attributes;
    }

    @Override
    public Optional<B> getBody() {
        return Optional.ofNullable(body);
    }

    @Override
    public MutableHttpResponse<B> body(@Nullable B body) {
        this.body = body;
        return this;
    }

    @Override
    public MutableHttpResponse<B> status(HttpStatus status, CharSequence message) {
        this.status = status;
        return this;
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }
}
