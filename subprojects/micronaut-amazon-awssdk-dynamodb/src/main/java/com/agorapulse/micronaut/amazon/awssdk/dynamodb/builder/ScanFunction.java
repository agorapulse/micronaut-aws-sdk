package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public interface ScanFunction<T> extends Function<Map<String, Object>, DetachedScan> {

    Consumer<ScanBuilder<T>> scan(Map<String, Object> args);

    @Override
    default DetachedScan apply(Map<String, Object> stringObjectMap) {
        return Builders.scan(scan(stringObjectMap));
    }

}
