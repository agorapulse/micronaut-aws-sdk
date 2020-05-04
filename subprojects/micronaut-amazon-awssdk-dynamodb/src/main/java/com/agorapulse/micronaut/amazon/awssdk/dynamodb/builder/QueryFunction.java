package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public interface QueryFunction<T> extends Function<Map<String, Object>, DetachedQuery> {

    Consumer<QueryBuilder<T>> query(Map<String, Object> args);

    @Override
    default DetachedQuery apply(Map<String, Object> stringObjectMap) {
        return Builders.query(query(stringObjectMap));
    }

}
