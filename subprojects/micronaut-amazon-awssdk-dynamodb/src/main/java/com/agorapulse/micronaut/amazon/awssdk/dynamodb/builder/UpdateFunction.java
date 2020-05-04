package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public interface UpdateFunction<T> extends Function<Map<String, Object>, DetachedUpdate> {

    Consumer<UpdateBuilder<T>> update(Map<String, Object> args);

    @Override
    default DetachedUpdate apply(Map<String, Object> stringObjectMap) {
        return Builders.update(update(stringObjectMap));
    }

}
