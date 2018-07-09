package com.agorapulse.micronaut.http.basic;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;

import java.net.URI;
import java.util.Set;

public interface HttpResponder {
    <T> MutableHttpResponse<T> ok();

    <T> MutableHttpResponse<T> notFound();

    <T> MutableHttpResponse<T> unauthorized();

    <T> MutableHttpResponse<T> notFound(T body);

    <T> MutableHttpResponse<T> badRequest();

    <T> MutableHttpResponse<T> badRequest(T body);

    <T> MutableHttpResponse<T> unprocessableEntity();

    <T> MutableHttpResponse<T> notAllowed(HttpMethod... allowed);

    <T> MutableHttpResponse<T> notAllowed(Set<HttpMethod> allowed);

    <T> MutableHttpResponse<T> serverError();

    <T> MutableHttpResponse<T> accepted();

    <T> MutableHttpResponse<T> noContent();

    <T> MutableHttpResponse<T> notModified();

    <T> MutableHttpResponse<T> ok(T body);

    <T> MutableHttpResponse<T> created(T body);

    <T> MutableHttpResponse<T> created(URI location);

    <T> MutableHttpResponse<T> seeOther(URI location);

    <T> MutableHttpResponse<T> temporaryRedirect(URI location);

    <T> MutableHttpResponse<T> permanentRedirect(URI location);

    <T> MutableHttpResponse<T> redirect(URI location);

    <T> MutableHttpResponse<T> status(HttpStatus status);

    <T> MutableHttpResponse<T> status(HttpStatus status, String reason);
}
