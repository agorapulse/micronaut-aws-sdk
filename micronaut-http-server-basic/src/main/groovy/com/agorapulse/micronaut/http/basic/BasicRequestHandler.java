package com.agorapulse.micronaut.http.basic;

import io.micronaut.buffer.netty.NettyByteBufferFactory;
import io.micronaut.context.BeanLocator;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.ReturnType;
import io.micronaut.core.util.StreamUtils;
import io.micronaut.http.*;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.http.server.binding.RequestArgumentSatisfier;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.InternalServerException;
import io.micronaut.http.server.types.files.FileCustomizableResponseType;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.http.codec.TextPlainCodec;
import io.micronaut.scheduling.executor.ExecutorSelector;
import io.micronaut.web.router.*;
import io.micronaut.web.router.exceptions.DuplicateRouteException;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import io.micronaut.web.router.resource.StaticResourceResolver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Simplified version of io.micronaut.http.server.netty.RoutingInBoundHandler.
 *
 * @since 1.0
 */
@Singleton
public class BasicRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BasicRequestHandler.class);

    private final Router router;
    private final ExecutorSelector executorSelector;
    private final StaticResourceResolver staticResourceResolver;
    private final ExecutorService ioExecutor;
    private final BeanLocator beanLocator;
    private final RequestArgumentSatisfier requestArgumentSatisfier;
    private final MediaTypeCodecRegistry mediaTypeCodecRegistry;

    /**
     * @param beanLocator               The bean locator
     * @param router                    The router
     * @param mediaTypeCodecRegistry    The media type codec registry
     * @param staticResourceResolver    The static resource resolver
     * @param requestArgumentSatisfier  The request argument satisfier
     * @param executorSelector          The executor selector
     * @param ioExecutor                The IO executor
     */
    public BasicRequestHandler(
        BeanLocator beanLocator,
        Router router,
        MediaTypeCodecRegistry mediaTypeCodecRegistry,
        StaticResourceResolver staticResourceResolver,
        RequestArgumentSatisfier requestArgumentSatisfier,
        ExecutorSelector executorSelector,
        ExecutorService ioExecutor
    ) {

        this.mediaTypeCodecRegistry = mediaTypeCodecRegistry;
        this.beanLocator = beanLocator;
        this.staticResourceResolver = staticResourceResolver;
        this.ioExecutor = ioExecutor;
        this.executorSelector = executorSelector;
        this.router = router;
        this.requestArgumentSatisfier = requestArgumentSatisfier;
    }

    @SuppressWarnings("unchecked")
    private Flowable<? extends HttpResponse> exceptionCaught(HttpRequest httpRequest, RouteMatch<?> originalRoute, Throwable cause) {
        RouteMatch<?> errorRoute = null;
        if (httpRequest == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Micronaut Server Error - No request state present. Cause: " + cause.getMessage(), cause);
            }
            return Flowable.just(HttpResponse.serverError());
        }

        // find the origination of of the route
        Class declaringType = null;
        if (originalRoute instanceof MethodExecutionHandle) {
            declaringType = ((MethodExecutionHandle) originalRoute).getDeclaringType();
        }

        // when arguments do not match, then there is UnsatisfiedRouteException, we can handle this with a routed bad request
        if (cause instanceof UnsatisfiedRouteException) {
            if (declaringType != null) {
                // handle error with a method that is non global with bad request
                errorRoute = router.route(declaringType, HttpStatus.BAD_REQUEST).orElse(null);
            }
            if (errorRoute == null) {
                // handle error with a method that is global with bad request
                errorRoute = router.route(HttpStatus.BAD_REQUEST).orElse(null);
            }
        }

        // any another other exception may arise. handle these with non global exception marked method or a global exception marked method.
        if (errorRoute == null) {
            if (declaringType != null) {
                errorRoute = router.route(declaringType, cause).orElse(null);
            }
            if (errorRoute == null) {
                errorRoute = router.route(cause).orElse(null);
            }
        }

        if (errorRoute != null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unexpected error occurred: " + cause.getMessage(), cause);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found matching exception handler for exception [{}]: {}", cause.getMessage(), errorRoute);
            }
            errorRoute = requestArgumentSatisfier.fulfillArgumentRequirements(errorRoute, httpRequest, false);
            MediaType defaultResponseMediaType = errorRoute.getProduces().stream().findFirst().orElse(MediaType.APPLICATION_JSON_TYPE);
            try {
                Object result = errorRoute.execute();
                io.micronaut.http.MutableHttpResponse<?> response = errorResultToResponse(result);
                MethodBasedRouteMatch<?, ?> methodBasedRoute = (MethodBasedRouteMatch) errorRoute;
                AtomicReference<HttpRequest<?>> requestReference = new AtomicReference<>(httpRequest);
                Flowable<MutableHttpResponse<?>> routePublisher = buildRoutePublisher(
                    methodBasedRoute.getDeclaringType(),
                    methodBasedRoute.getReturnType().getType(),
                    requestReference,
                    Flowable.just(response));

                Flowable<? extends MutableHttpResponse<?>> filteredPublisher = filterPublisher(
                    requestReference,
                    routePublisher,
                    ioExecutor);

                return subscribeToResponsePublisher(
                    defaultResponseMediaType,
                    filteredPublisher
                );

            } catch (Throwable e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Exception occurred executing error handler. Falling back to default error handling: " + e.getMessage(), e);
                }
                return writeDefaultErrorResponse(e);
            }
        } else {
            Optional<ExceptionHandler> exceptionHandler = beanLocator
                .findBean(ExceptionHandler.class, Qualifiers.byTypeArguments(cause.getClass(), Object.class));

            if (exceptionHandler.isPresent()) {
                ExceptionHandler handler = exceptionHandler.get();
                MediaType defaultResponseMediaType = MediaType.fromType(exceptionHandler.getClass()).orElse(MediaType.APPLICATION_JSON_TYPE);
                try {
                    Object result = handler.handle(httpRequest, cause);
                    AtomicReference<HttpRequest<?>> requestReference = new AtomicReference<>(httpRequest);
                    io.micronaut.http.MutableHttpResponse response = errorResultToResponse(result);
                    Flowable<MutableHttpResponse<?>> routePublisher = buildRoutePublisher(
                        handler.getClass(),
                        result != null ? result.getClass() : HttpResponse.class,
                        requestReference,
                        Flowable.just(response));

                    Flowable<? extends MutableHttpResponse<?>> filteredPublisher = filterPublisher(
                        requestReference,
                        routePublisher,
                        ioExecutor);

                    return subscribeToResponsePublisher(
                        defaultResponseMediaType,
                        filteredPublisher
                    );
                } catch (Throwable e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Exception occurred executing error handler. Falling back to default error handling.");
                    }
                    return writeDefaultErrorResponse(e);
                }
            } else {
                return writeDefaultErrorResponse(cause);
            }
        }
    }

    public HttpResponse handleRequest(io.micronaut.http.HttpRequest<?> request) {
        try {
            return handleRequestInternal(request).blockingFirst();
        } catch (Throwable throwable) {
            return exceptionCaught(request, (RouteMatch) request.getAttribute(HttpAttributes.ROUTE_MATCH).get(), throwable).blockingFirst();
        }
    }

    private Flowable<? extends HttpResponse> handleRequestInternal(io.micronaut.http.HttpRequest<?> request) {
        io.micronaut.http.HttpMethod httpMethod = request.getMethod();
        String requestPath = request.getPath();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Matching route {} - {}", httpMethod, requestPath);
        }

        Optional<UriRouteMatch<Object, Object>> routeMatch = Optional.empty();

        List<UriRouteMatch<Object, Object>> uriRoutes = router
            .find(httpMethod, requestPath)
            .filter(match -> match.test(request))
            .collect(StreamUtils.minAll(
                Comparator.comparingInt(match -> match.getVariableValues().size()),
                Collectors.toList()));

        if (uriRoutes.size() > 1) {
            throw new DuplicateRouteException(requestPath, uriRoutes);
        } else if (uriRoutes.size() == 1) {
            UriRouteMatch<Object, Object> establishedRoute = uriRoutes.get(0);
            request.setAttribute(HttpAttributes.ROUTE, establishedRoute.getRoute());
            request.setAttribute(HttpAttributes.ROUTE_MATCH, establishedRoute);
            request.setAttribute(HttpAttributes.URI_TEMPLATE, establishedRoute.getRoute().getUriMatchTemplate().toString());
            routeMatch = Optional.of(establishedRoute);
        }

        RouteMatch<?> route;

        if (!routeMatch.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No matching route found for URI {} and method {}", request.getPath(), httpMethod);
            }

            // if there is no route present try to locate a route that matches a different HTTP method
            Set<io.micronaut.http.HttpMethod> existingRoutes = router
                .findAny(request.getPath())
                .map(UriRouteMatch::getHttpMethod)
                .collect(Collectors.toSet());

            if (!existingRoutes.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Method not allowed for URI {} and method {}", request.getPath(), httpMethod);
                }

                return handleStatusError(
                    request,
                    HttpResponse.notAllowed(existingRoutes),
                    "Method [" + httpMethod + "] not allowed. Allowed methods: " + existingRoutes);
            } else {
                Optional<? extends FileCustomizableResponseType> optionalFile = matchFile(requestPath);

                if (optionalFile.isPresent()) {
                    route = new BasicObjectRouteMatch(optionalFile.get());
                } else {
                    Optional<RouteMatch<Object>> statusRoute = router.route(HttpStatus.NOT_FOUND);
                    if (statusRoute.isPresent()) {
                        route = statusRoute.get();
                    } else {
                        return emitDefaultNotFoundResponse(request);
                    }
                }
            }
        } else {
            route = routeMatch.get();
        }
        // Check that the route is an accepted content type
        MediaType contentType = request.getContentType().orElse(null);
        if (!route.accept(contentType)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Matched route is not a supported media type: {}", contentType);
            }

            return handleStatusError(
                request,
                HttpResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
                "Unsupported Media Type: " + contentType);
        }
        if (LOG.isDebugEnabled()) {
            if (route instanceof MethodBasedRouteMatch) {
                LOG.debug("Matched route {} - {} to controller {}", httpMethod, requestPath, route.getDeclaringType());
            } else {
                LOG.debug("Matched route {} - {}", httpMethod, requestPath);
            }
        }
        // all ok proceed to try and execute the route
        return handleRouteMatch(route, request);
    }

    private Flowable<? extends HttpResponse> handleStatusError(
        HttpRequest<?> request,
        MutableHttpResponse<Object> defaultResponse,
        String message) {
        Optional<RouteMatch<Object>> statusRoute = router.route(defaultResponse.status());
        if (statusRoute.isPresent()) {
            RouteMatch<Object> routeMatch = statusRoute.get();
            return handleRouteMatch(routeMatch, request);
        } else {

            if (HttpMethod.permitsRequestBody(request.getMethod())) {
                JsonError error = newError(request, message);
                defaultResponse.body(error);
            }


            AtomicReference<HttpRequest<?>> requestReference = new AtomicReference<>(request);
            Flowable<? extends MutableHttpResponse<?>> responsePublisher = filterPublisher(
                requestReference,
                Flowable.just(defaultResponse),
                ioExecutor
            );
            return subscribeToResponsePublisher(
                MediaType.APPLICATION_JSON_TYPE,
                responsePublisher
            );
        }
    }

    private Optional<? extends FileCustomizableResponseType> matchFile(String path) {
        Optional<URL> optionalUrl = staticResourceResolver.resolve(path);

        if (optionalUrl.isPresent()) {
            try {
                URL url = optionalUrl.get();
                if (url.getProtocol().equals("file")) {
                    File file = Paths.get(url.toURI()).toFile();
                    if (file.exists() && !file.isDirectory() && file.canRead()) {
                        return Optional.of(new BasicSystemFileCustomizableResponseType(file));
                    }
                }

                return Optional.empty();
            } catch (URISyntaxException e) {
                //no-op
            }
        }

        return Optional.empty();
    }

    private Flowable<? extends HttpResponse> emitDefaultNotFoundResponse(io.micronaut.http.HttpRequest<?> request) {
        MutableHttpResponse<Object> res = newNotFoundError(request);
        AtomicReference<HttpRequest<?>> requestReference = new AtomicReference<>(request);
        Flowable<? extends MutableHttpResponse<?>> responsePublisher = filterPublisher(
            requestReference,
            Flowable.just(res),
            ioExecutor
        );
        return subscribeToResponsePublisher(
            MediaType.APPLICATION_JSON_TYPE,
            responsePublisher
        );
    }

    private MutableHttpResponse<Object> newNotFoundError(HttpRequest<?> request) {
        JsonError error = newError(request, "Page Not Found");
        return HttpResponse.notFound()
            .body(error);
    }

    private JsonError newError(io.micronaut.http.HttpRequest<?> request, String message) {
        URI uri = request.getUri();
        return new JsonError(message)
            .link(Link.SELF, Link.of(uri));
    }

    private MutableHttpResponse errorResultToResponse(Object result) {
        MutableHttpResponse<?> response;
        if (result == null) {
            response = HttpResponse.serverError();
        } else if (result instanceof io.micronaut.http.HttpResponse) {
            response = (MutableHttpResponse) result;
        } else {
            response = HttpResponse.serverError()
                .body(result);
            MediaType.fromType(result.getClass()).ifPresent(response::contentType);
        }
        return response;
    }

    private Flowable<? extends HttpResponse> handleRouteMatch(
        RouteMatch<?> route,
        HttpRequest<?> request
    ) {
        // try to fulfill the argument requirements of the route
        route = requestArgumentSatisfier.fulfillArgumentRequirements(route, request, false);

        try {
            AtomicReference<Flowable<? extends HttpResponse>> flowableReference = new AtomicReference<>();
            route = prepareRouteForExecution(route, request, flowableReference);
            route.execute();
            return flowableReference.get();
        } catch (Exception e) {
            return exceptionCaught(request, route, e);
        }
    }

    private RouteMatch<?> prepareRouteForExecution(RouteMatch<?> route, HttpRequest<?> request, AtomicReference<Flowable<? extends HttpResponse>> reference) {
        // Select the most appropriate Executor
        ExecutorService executor;
        if (route instanceof MethodBasedRouteMatch) {
            executor = executorSelector.select((MethodBasedRouteMatch) route).orElse(ioExecutor);
        } else {
            executor = ioExecutor;
        }

        route = route.decorate(finalRoute -> {
            MediaType defaultResponseMediaType = finalRoute
                .getProduces()
                .stream()
                .findFirst()
                .orElse(MediaType.APPLICATION_JSON_TYPE);


            ReturnType<?> genericReturnType = finalRoute.getReturnType();
            Class<?> javaReturnType = genericReturnType.getType();

            AtomicReference<io.micronaut.http.HttpRequest<?>> requestReference = new AtomicReference<>(request);
            boolean isFuture = CompletableFuture.class.isAssignableFrom(javaReturnType);
            boolean isReactiveReturnType = Publishers.isConvertibleToPublisher(javaReturnType) || isFuture;
            boolean isSingle =
                isReactiveReturnType && Publishers.isSingle(javaReturnType)
                    || isResponsePublisher(genericReturnType, javaReturnType)
                    || isFuture
                    || finalRoute.getAnnotationMetadata().getValue(Produces.class, "single", Boolean.class).orElse(false);

            // build the result emitter. This result emitter emits the response from a controller action
            Flowable<?> resultEmitter = buildResultEmitter(finalRoute, requestReference, isReactiveReturnType, isSingle);


            // here we transform the result of the controller action into a MutableHttpResponse
            Flowable<MutableHttpResponse<?>> routePublisher = resultEmitter.map((message) -> {
                HttpResponse<?> response = messageToResponse(finalRoute, message);
                MutableHttpResponse<?> finalResponse = (MutableHttpResponse<?>) response;
                HttpStatus status = finalResponse.getStatus();
                if (status.getCode() >= HttpStatus.BAD_REQUEST.getCode()) {
                    Class declaringType = ((MethodBasedRouteMatch) finalRoute).getDeclaringType();
                    // handle re-mapping of errors
                    Optional<RouteMatch<Object>> statusRoute = Optional.empty();
                    // if declaringType is not null, this means its a locally marked method handler
                    if (declaringType != null) {
                        statusRoute = router.route(declaringType, status);
                    }
                    if (!statusRoute.isPresent()) {
                        statusRoute = router.route(status);
                    }
                    io.micronaut.http.HttpRequest<?> httpRequest = requestReference.get();

                    if (statusRoute.isPresent()) {
                        RouteMatch<Object> newRoute = statusRoute.get();
                        requestArgumentSatisfier.fulfillArgumentRequirements(newRoute, httpRequest, true);

                        if (newRoute.isExecutable()) {
                            Object result;
                            try {
                                result = newRoute.execute();
                                finalResponse = messageToResponse(newRoute, result);
                            } catch (Throwable e) {
                                throw new InternalServerException("Error executing status route [" + newRoute + "]: " + e.getMessage(), e);
                            }
                        }
                    }

                }
                return finalResponse;
            });

            routePublisher = buildRoutePublisher(
                finalRoute.getDeclaringType(),
                javaReturnType,
                requestReference,
                routePublisher
            );

            // process the publisher through the available filters
            Flowable<? extends MutableHttpResponse<?>> filteredPublisher = filterPublisher(
                requestReference,
                routePublisher,
                executor
            );

            boolean isStreaming = isReactiveReturnType && !isSingle;

            filteredPublisher = filteredPublisher.switchMap((response) -> {
                Optional<?> responseBody = response.getBody();
                if (responseBody.isPresent()) {
                    Object body = responseBody.get();
                    if (isStreaming) {
                        // handled downstream
                        return Flowable.just(response);
                    } else if (Publishers.isConvertibleToPublisher(body)) {
                        Flowable<?> bodyFlowable = Publishers.convertPublisher(body, Flowable.class);
                        Flowable<MutableHttpResponse<?>> bodyToResponse = bodyFlowable.map((bodyContent) ->
                            setBodyContent(response, bodyContent)
                        );
                        return bodyToResponse.switchIfEmpty(Flowable.just(response));
                    }
                }

                return Flowable.just(response);
            });

            reference.set(subscribeToResponsePublisher(defaultResponseMediaType, filteredPublisher));

            return null;
        });
        return route;
    }

    private Flowable<MutableHttpResponse<?>> buildRoutePublisher(
        Class<?> declaringType,
        Class<?> javaReturnType,
        AtomicReference<HttpRequest<?>> requestReference,
        Flowable<MutableHttpResponse<?>> routePublisher) {
        // In the case of an empty reactive type we switch handling so that
        // a 404 NOT_FOUND is returned
        routePublisher = routePublisher.switchIfEmpty(Flowable.create((emitter) -> {
            HttpRequest<?> httpRequest = requestReference.get();
            MutableHttpResponse<?> response;
            if (javaReturnType != void.class) {

                // handle re-mapping of errors
                Optional<RouteMatch<Object>> statusRoute = Optional.empty();
                // if declaringType is not null, this means its a locally marked method handler
                if (declaringType != null) {
                    statusRoute = router.route(declaringType, HttpStatus.NOT_FOUND);
                }
                if (!statusRoute.isPresent()) {
                    statusRoute = router.route(HttpStatus.NOT_FOUND);
                }

                if (statusRoute.isPresent()) {
                    RouteMatch<Object> newRoute = statusRoute.get();
                    requestArgumentSatisfier.fulfillArgumentRequirements(newRoute, httpRequest, true);

                    if (newRoute.isExecutable()) {
                        try {
                            Object result = newRoute.execute();
                            response = messageToResponse(newRoute, result);
                        } catch (Throwable e) {
                            emitter.onError(new InternalServerException("Error executing status route [" + newRoute + "]: " + e.getMessage(), e));
                            return;
                        }

                    } else {
                        response = newNotFoundError(httpRequest);
                    }
                } else {
                    response = newNotFoundError(httpRequest);
                }
            } else {
                // void return type with no response, nothing else to do
                response = HttpResponse.ok();
            }
            try {
                emitter.onNext(response);
                emitter.onComplete();
            } catch (Throwable e) {
                emitter.onError(new InternalServerException("Error executing Error route [" + response.getStatus() + "]: " + e.getMessage(), e));
            }
        }, BackpressureStrategy.ERROR));
        return routePublisher;
    }

    private Flowable<? extends HttpResponse> subscribeToResponsePublisher(
        MediaType defaultResponseMediaType,
        Flowable<? extends MutableHttpResponse<?>> finalPublisher) {
        finalPublisher = finalPublisher.map((response) -> {
            Optional<MediaType> specifiedMediaType = response.getContentType();
            MediaType responseMediaType = specifiedMediaType.orElse(defaultResponseMediaType);

            Optional<?> responseBody = response.getBody();
            if (responseBody.isPresent()) {

                Object body = responseBody.get();

                if (specifiedMediaType.isPresent()) {

                    Optional<MediaTypeCodec> registeredCodec = mediaTypeCodecRegistry.findCodec(responseMediaType, body.getClass());
                    if (registeredCodec.isPresent()) {
                        MediaTypeCodec codec = registeredCodec.get();
                        return encodeBodyWithCodec(response, body, codec, responseMediaType);
                    }
                }

                Optional<MediaTypeCodec> registeredCodec = mediaTypeCodecRegistry.findCodec(defaultResponseMediaType, body.getClass());
                if (registeredCodec.isPresent()) {
                    MediaTypeCodec codec = registeredCodec.get();
                    return encodeBodyWithCodec(response, body, codec, responseMediaType);
                }

                MediaTypeCodec defaultCodec = new TextPlainCodec(Charset.defaultCharset());

                return encodeBodyWithCodec(response, body, defaultCodec, responseMediaType);
            } else {
                return response;
            }
        });

        return finalPublisher;
    }

    private MutableHttpResponse<?> encodeBodyWithCodec(MutableHttpResponse<?> response, Object body, MediaTypeCodec codec, MediaType mediaType) {
        ByteBuf byteBuf = encodeBodyAsByteBuf(body, codec);
        int len = byteBuf.readableBytes();
        MutableHttpHeaders headers = response.getHeaders();
        if (!headers.contains(HttpHeaders.CONTENT_TYPE)) {
            headers.add(HttpHeaders.CONTENT_TYPE, mediaType);
        }
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(len));

        setBodyContent(response, byteBuf);
        return response;
    }

    private MutableHttpResponse<?> setBodyContent(MutableHttpResponse response, Object bodyContent) {
        @SuppressWarnings("unchecked")
        MutableHttpResponse<?> res = response.body(bodyContent);
        return res;
    }

    private ByteBuf encodeBodyAsByteBuf(Object body, MediaTypeCodec codec) {
        ByteBuf byteBuf;
        if (body instanceof ByteBuf) {
            byteBuf = (ByteBuf) body;
        } else if (body instanceof ByteBuffer) {
            ByteBuffer byteBuffer = (ByteBuffer) body;
            Object nativeBuffer = byteBuffer.asNativeBuffer();
            if (nativeBuffer instanceof ByteBuf) {
                byteBuf = (ByteBuf) nativeBuffer;
            } else {
                byteBuf = Unpooled.copiedBuffer(byteBuffer.asNioBuffer());
            }
        } else if (body instanceof byte[]) {
            byteBuf = Unpooled.copiedBuffer((byte[]) body);
        } else {
            byteBuf = (ByteBuf) codec.encode(body, new NettyByteBufferFactory(ByteBufAllocator.DEFAULT)).asNativeBuffer();
        }
        return byteBuf;
    }

    // builds the result emitter for a given route action
    private Flowable<?> buildResultEmitter(
        RouteMatch<?> finalRoute,
        AtomicReference<HttpRequest<?>> requestReference,
        boolean isReactiveReturnType,
        boolean isSingleResult) {
        Flowable<?> resultEmitter;
        if (isReactiveReturnType) {
            // if the return type is reactive, execute the action and obtain the Observable
            RouteMatch<?> routeMatch = finalRoute;
            if (!routeMatch.isExecutable()) {
                routeMatch = requestArgumentSatisfier.fulfillArgumentRequirements(routeMatch, requestReference.get(), true);
            }
            try {
                if (isSingleResult) {
                    // for a single result we are fine as is
                    resultEmitter = Flowable.defer(() -> {
                        Object result = finalRoute.execute();
                        return Publishers.convertPublisher(result, Publisher.class);
                    });
                } else {
                    // for a streaming response we wrap the result on an HttpResponse so that a single result is received
                    // then the result can be streamed chunk by chunk
                    resultEmitter = Flowable.create((emitter) -> {
                        Object result = finalRoute.execute();
                        MutableHttpResponse<Object> chunkedResponse = HttpResponse.ok(result);
                        chunkedResponse.header(HttpHeaders.TRANSFER_ENCODING, "chunked");
                        emitter.onNext(chunkedResponse);
                        emitter.onComplete();
                        // should be no back pressure
                    }, BackpressureStrategy.ERROR);
                }
            } catch (Throwable e) {
                resultEmitter = Flowable.error(new InternalServerException("Error executing route [" + routeMatch + "]: " + e.getMessage(), e));
            }
        } else {
            // for non-reactive results we build flowable that executes the
            // route
            resultEmitter = Flowable.create((emitter) -> {
                HttpRequest<?> httpRequest = requestReference.get();
                RouteMatch<?> routeMatch = finalRoute;
                if (!routeMatch.isExecutable()) {
                    routeMatch = requestArgumentSatisfier.fulfillArgumentRequirements(routeMatch, httpRequest, true);
                }
                Object result;
                try {
                    result = routeMatch.execute();
                } catch (Throwable e) {
                    emitter.onError(e);
                    return;
                }

                if (result == null) {
                    // empty flowable
                    emitter.onComplete();
                } else {
                    // emit the result
                    emitter.onNext(result);
                    emitter.onComplete();
                }

                // should be no back pressure
            }, BackpressureStrategy.ERROR);
        }
        return resultEmitter;
    }

    private MutableHttpResponse<?> messageToResponse(RouteMatch<?> finalRoute, Object message) {
        MutableHttpResponse<?> response;
        if (message instanceof HttpResponse) {
            response = ConversionService.SHARED.convert(message, MutableHttpResponse.class)
                .orElseThrow(() -> new InternalServerException("Emitted response is not mutable"));
        } else {
            if (message instanceof HttpStatus) {
                response = HttpResponse.status((HttpStatus) message);
            } else {
                HttpStatus status = HttpStatus.OK;

                if (finalRoute instanceof MethodBasedRouteMatch) {
                    final MethodBasedRouteMatch rm = (MethodBasedRouteMatch) finalRoute;
                    if (rm.hasAnnotation(Status.class)) {
                        status = rm.getAnnotation(Status.class).getValue(HttpStatus.class).get();
                    }
                }
                response = HttpResponse.status(status).body(message);
            }
        }
        return response;
    }

    private boolean isResponsePublisher(ReturnType<?> genericReturnType, Class<?> javaReturnType) {
        return Publishers.isConvertibleToPublisher(javaReturnType) && genericReturnType.getFirstTypeVariable().map(arg -> HttpResponse.class.isAssignableFrom(arg.getType())).orElse(false);
    }

    private Flowable<? extends MutableHttpResponse<?>> filterPublisher(
        AtomicReference<HttpRequest<?>> requestReference,
        Publisher<MutableHttpResponse<?>> routePublisher, ExecutorService executor) {
        Publisher<? extends io.micronaut.http.MutableHttpResponse<?>> finalPublisher;
        List<HttpFilter> filters = new ArrayList<>(router.findFilters(requestReference.get()));
        if (!filters.isEmpty()) {
            // make the action executor the last filter in the chain
            filters.add((HttpServerFilter) (req, chain) -> routePublisher);

            AtomicInteger integer = new AtomicInteger();
            int len = filters.size();
            ServerFilterChain filterChain = new ServerFilterChain() {
                @SuppressWarnings("unchecked")
                @Override
                public Publisher<MutableHttpResponse<?>> proceed(io.micronaut.http.HttpRequest<?> request) {
                    int pos = integer.incrementAndGet();
                    if (pos > len) {
                        throw new IllegalStateException("The FilterChain.proceed(..) method should be invoked exactly once per filter execution. The method has instead been invoked multiple times by an erroneous filter definition.");
                    }
                    HttpFilter httpFilter = filters.get(pos);
                    return (Publisher<MutableHttpResponse<?>>) httpFilter.doFilter(requestReference.getAndSet(request), this);
                }
            };
            HttpFilter httpFilter = filters.get(0);
            Publisher<? extends HttpResponse<?>> resultingPublisher = httpFilter.doFilter(requestReference.get(), filterChain);
            finalPublisher = (Publisher<? extends MutableHttpResponse<?>>) resultingPublisher;
        } else {
            finalPublisher = routePublisher;
        }

        // Handle the scheduler to subscribe on
        if (finalPublisher instanceof Flowable) {
            return ((Flowable<MutableHttpResponse<?>>) finalPublisher)
                .subscribeOn(Schedulers.from(executor));
        } else {
            return Flowable.fromPublisher(finalPublisher)
                .subscribeOn(Schedulers.from(executor));
        }
    }

    @SuppressWarnings("unchecked")
    private Flowable<? extends HttpResponse> writeDefaultErrorResponse(Throwable cause) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected error occurred: " + cause.getMessage(), cause);
        }

        MutableHttpResponse<?> error = HttpResponse.serverError()
            .body(new JsonError("Internal Server Error: " + cause.getMessage()));
        return subscribeToResponsePublisher(
            MediaType.APPLICATION_JSON_TYPE,
            Flowable.just(error)
        );
    }
}
