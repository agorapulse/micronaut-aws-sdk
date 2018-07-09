package com.agorapulse.micronaut.http.basic;

import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;

public class BasicHttpResponseFactory implements HttpResponseFactory {

    @Override
    public <T> MutableHttpResponse<T> ok(T body) {
        return new BasicHttpResponse<>();
    }

    @Override
    public <T> MutableHttpResponse<T> status(HttpStatus status, String reason) {
        return new BasicHttpResponse<T>().status(status, reason);
    }

    @Override
    public <T> MutableHttpResponse<T> status(HttpStatus status, T body) {
        return new BasicHttpResponse<T>().status(status).body(body);
    }
}
