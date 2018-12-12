package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;
import space.jasan.support.groovy.closure.FunctionWithDelegate;

import java.util.function.Consumer;
import java.util.function.Function;

public interface UpdateBuilder<T> extends DetachedUpdate<T> {

    UpdateBuilder<T> hash(Object key);

    UpdateBuilder<T> range(Object range);

    UpdateBuilder<T> add(String attributeName, Object delta);

    UpdateBuilder<T> put(String attributeName, Object value);

    UpdateBuilder<T> delete(String attributeName);

    UpdateBuilder<T> returns(ReturnValue returnValue, Function<T, ?> mapper);

    default UpdateBuilder<T> returnNone() {
        return returns(ReturnValue.NONE, Function.identity());
    }

    default UpdateBuilder<T> returnAllOld(Function<T, ?> mapper) {
        return returns(ReturnValue.ALL_OLD, mapper);
    }

    default UpdateBuilder<T> returnAllOld(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.ALL_OLD, FunctionWithDelegate.create(mapper));
    }

    default UpdateBuilder<T> returnUpdatedOld(Function<T, ?> mapper) {
        return returns(ReturnValue.UPDATED_OLD, mapper);
    }

    default UpdateBuilder<T> returnUpdatedOld(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.UPDATED_OLD, FunctionWithDelegate.create(mapper));
    }

    default UpdateBuilder<T> returnAllNew(Function<T, ?> mapper) {
        return returns(ReturnValue.ALL_NEW, mapper);
    }

    default UpdateBuilder<T> returnAllNew(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.ALL_NEW, FunctionWithDelegate.create(mapper));
    }

    default UpdateBuilder<T> returnUpdatedNew(Function<T, ?> mapper) {
        return returns(ReturnValue.UPDATED_NEW, mapper);
    }

    default UpdateBuilder<T> returnUpdatedNew(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.UPDATED_NEW, FunctionWithDelegate.create(mapper));
    }

    default UpdateBuilder<T> returns(ReturnValue returnValue) {
        return returns(returnValue, Function.identity());
    }

    default UpdateBuilder<T> returns(
        ReturnValue returnValue,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(returnValue, FunctionWithDelegate.create(mapper));
    }

    UpdateBuilder<T> configure(Consumer<UpdateItemRequest> configurer);

    default UpdateBuilder<T> configure(
        @DelegatesTo(type = "com.amazonaws.services.dynamodbv2.model.UpdateItemRequest", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.amazonaws.services.dynamodbv2.model.UpdateItemRequest")
            Closure<Object> configurer
    ) {
        return configure(ConsumerWithDelegate.create(configurer));
    }

}
