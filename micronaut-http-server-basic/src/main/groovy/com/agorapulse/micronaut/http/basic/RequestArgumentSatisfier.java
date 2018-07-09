package com.agorapulse.micronaut.http.basic;

import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.binding.RequestBinderRegistry;
import io.micronaut.http.server.binding.binders.BodyArgumentBinder;
import io.micronaut.http.server.binding.binders.NonBlockingBodyArgumentBinder;
import io.micronaut.web.router.RouteMatch;
import io.micronaut.web.router.UnresolvedArgument;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A class containing methods to aid in satisfying arguments of a {@link io.micronaut.web.router.Route}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RequestArgumentSatisfier {

    private final RequestBinderRegistry binderRegistry;

    /**
     * @param requestBinderRegistry The Request binder registry
     */
    RequestArgumentSatisfier(RequestBinderRegistry requestBinderRegistry) {
        this.binderRegistry = requestBinderRegistry;
    }

    /**
     * Attempt to satisfy the arguments of the given route with the data from the given request.
     *
     * @param route            The route
     * @param request          The request
     * @param satisfyOptionals Whether to satisfy optionals
     * @return The route
     */
    RouteMatch<?> fulfillArgumentRequirements(RouteMatch<?> route, HttpRequest<?> request, boolean satisfyOptionals) {
        Collection<Argument> requiredArguments = route.getRequiredArguments();
        Map<String, Object> argumentValues;

        if (requiredArguments.isEmpty()) {
            // no required arguments so just execute
            argumentValues = Collections.emptyMap();
        } else {
            argumentValues = new LinkedHashMap<>();
            // Begin try fulfilling the argument requirements
            for (Argument argument : requiredArguments) {
                getValueForArgument(argument, request, satisfyOptionals).ifPresent((value) ->
                    argumentValues.put(argument.getName(), value));
            }
        }

        route = route.fulfill(argumentValues);
        return route;
    }

    /**
     * @param argument         The argument
     * @param request          The HTTP request
     * @param satisfyOptionals Whether to satisfy optionals
     * @return An {@link Optional} for the value
     */
    protected Optional<Object> getValueForArgument(Argument argument, HttpRequest<?> request, boolean satisfyOptionals) {
        Object value = null;
        Optional<ArgumentBinder> registeredBinder =
            binderRegistry.findArgumentBinder(argument, request);
        if (registeredBinder.isPresent()) {
            ArgumentBinder argumentBinder = registeredBinder.get();
            ArgumentConversionContext conversionContext = ConversionContext.of(
                argument,
                request.getLocale().orElse(null),
                request.getCharacterEncoding()
            );

            if (argumentBinder instanceof BodyArgumentBinder) {
                if (argumentBinder instanceof NonBlockingBodyArgumentBinder) {
                    ArgumentBinder.BindingResult bindingResult = argumentBinder
                        .bind(conversionContext, request);

                    if (bindingResult.isPresentAndSatisfied()) {
                        value = bindingResult.get();
                    }

                } else {
                    value = (UnresolvedArgument) () -> argumentBinder.bind(conversionContext, request);
                }
            } else {

                ArgumentBinder.BindingResult bindingResult = argumentBinder
                    .bind(conversionContext, request);
                if (argument.getType() == Optional.class) {
                    if (bindingResult.isSatisfied() || satisfyOptionals) {
                        Optional optionalValue = bindingResult.getValue();
                        if (optionalValue.isPresent()) {
                            value = optionalValue.get();
                        } else {
                            value = optionalValue;
                        }
                    }
                } else if (bindingResult.isPresentAndSatisfied()) {
                    value = bindingResult.get();
                } else if (HttpMethod.requiresRequestBody(request.getMethod()) || argument.getDeclaredAnnotation(Nullable.class) != null) {
                    value = (UnresolvedArgument) () -> {
                        ArgumentBinder.BindingResult result = argumentBinder.bind(conversionContext, request);
                        Optional<ConversionError> lastError = conversionContext.getLastError();
                        if (lastError.isPresent()) {
                            return (ArgumentBinder.BindingResult) () -> lastError;
                        }
                        return result;
                    };
                }
            }
        }
        return Optional.ofNullable(value);
    }
}
