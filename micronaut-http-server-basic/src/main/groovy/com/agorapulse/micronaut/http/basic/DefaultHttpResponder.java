package com.agorapulse.micronaut.http.basic;

import io.micronaut.context.annotation.Secondary;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Set;

@Singleton
@Secondary
public class DefaultHttpResponder implements HttpResponder {

    public DefaultHttpResponder(HttpResponseFactory factory) {
        this.factory = factory;
    }

    /**
     * Return an {@link HttpStatus#OK} response with an empty body.
     *
     * @param <T> The response type
     * @return The ok response
     */
    @Override
    public <T> MutableHttpResponse<T> ok() {
        return this.factory.ok();
    }

    /**
     * Return an {@link HttpStatus#NOT_FOUND} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> notFound() {
        return this.factory.status(HttpStatus.NOT_FOUND);
    }

    /**
     * Return an {@link HttpStatus#UNAUTHORIZED} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> unauthorized() {
        return this.factory.status(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Return an {@link HttpStatus#NOT_FOUND} response with a body.
     *
     * @param body The response body
     * @param <T>  The body type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> notFound(T body) {
        return this.factory.<T>status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Return an {@link HttpStatus#BAD_REQUEST} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> badRequest() {
        return this.factory.status(HttpStatus.BAD_REQUEST);
    }

    /**
     * Return an {@link HttpStatus#BAD_REQUEST} response with an empty body.
     *
     * @param body The response body
     * @param <T>  The body type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> badRequest(T body) {
        return this.factory.status(HttpStatus.BAD_REQUEST, body);
    }

    /**
     * Return an {@link HttpStatus#UNPROCESSABLE_ENTITY} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> unprocessableEntity() {
        return this.factory.status(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Return an {@link HttpStatus#METHOD_NOT_ALLOWED} response with an empty body.
     *
     * @param allowed Allowed Http Methods
     * @param <T>     The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> notAllowed(HttpMethod... allowed) {
        return this.factory.<T>status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers -> headers.allow(allowed));
    }

    /**
     * Return an {@link HttpStatus#METHOD_NOT_ALLOWED} response with an empty body.
     *
     * @param allowed Allowed Http Methods
     * @param <T>     The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> notAllowed(Set<HttpMethod> allowed) {
        return this.factory.<T>status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers -> headers.allow(allowed));
    }

    /**
     * Return an {@link HttpStatus#INTERNAL_SERVER_ERROR} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> serverError() {
        return this.factory.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Return an {@link HttpStatus#ACCEPTED} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> accepted() {
        return this.factory.status(HttpStatus.ACCEPTED);
    }

    /**
     * Return an {@link HttpStatus#NO_CONTENT} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> noContent() {
        return this.factory.status(HttpStatus.NO_CONTENT);
    }

    /**
     * Return an {@link HttpStatus#NOT_MODIFIED} response with an empty body.
     *
     * @param <T> The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> notModified() {
        return this.factory.status(HttpStatus.NOT_MODIFIED);
    }

    /**
     * Return an {@link HttpStatus#OK} response with a body.
     *
     * @param body The response body
     * @param <T>  The body type
     * @return The ok response
     */
    @Override
    public <T> MutableHttpResponse<T> ok(T body) {
        return this.factory.ok(body);
    }

    /**
     * Return an {@link HttpStatus#CREATED} response with a body.
     *
     * @param body The response body
     * @param <T>  The body type
     * @return The created response
     */
    @Override
    public <T> MutableHttpResponse<T> created(T body) {
        return this.factory.<T>status(HttpStatus.CREATED).body(body);
    }

    /**
     * Return an {@link HttpStatus#CREATED} response with the location of the new resource.
     *
     * @param location The location of the new resource
     * @param <T>      The response type
     * @return The created response
     */
    @Override
    public <T> MutableHttpResponse<T> created(URI location) {
        return this.factory.<T>status(HttpStatus.CREATED).headers(headers -> headers.location(location));
    }

    /**
     * Return an {@link HttpStatus#SEE_OTHER} response with the location of the new resource.
     *
     * @param location The location of the new resource
     * @param <T>      The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> seeOther(URI location) {
        return this.factory.<T>status(HttpStatus.SEE_OTHER).headers(headers -> headers.location(location));
    }

    /**
     * Return an {@link HttpStatus#TEMPORARY_REDIRECT} response with the location of the new resource.
     *
     * @param location The location of the new resource
     * @param <T>      The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> temporaryRedirect(URI location) {
        return this.factory.<T>status(HttpStatus.TEMPORARY_REDIRECT).headers(headers -> headers.location(location));
    }

    /**
     * Return an {@link HttpStatus#PERMANENT_REDIRECT} response with the location of the new resource.
     *
     * @param location The location of the new resource
     * @param <T>      The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> permanentRedirect(URI location) {
        return this.factory.<T>status(HttpStatus.PERMANENT_REDIRECT).headers(headers -> headers.location(location));
    }

    /**
     * Return an {@link HttpStatus#MOVED_PERMANENTLY} response with the location of the new resource.
     *
     * @param location The location of the new resource
     * @param <T>      The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> redirect(URI location) {
        return this.factory.<T>status(HttpStatus.MOVED_PERMANENTLY).headers(headers -> headers.location(location));
    }

    /**
     * Return a response for the given status.
     *
     * @param status The status
     * @param <T>    The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> status(HttpStatus status) {
        return this.factory.status(status);
    }

    /**
     * Return a response for the given status.
     *
     * @param status The status
     * @param reason An alternatively reason message
     * @param <T>    The response type
     * @return The response
     */
    @Override
    public <T> MutableHttpResponse<T> status(HttpStatus status, String reason) {
        return this.factory.status(status, reason);
    }

    private HttpResponseFactory factory;

}
