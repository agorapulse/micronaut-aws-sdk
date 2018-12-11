package com.agorapulse.micronaut.aws.dynamodb.annotation;

import com.agorapulse.micronaut.aws.dynamodb.query.DetachedQuery;

import java.util.Map;
import java.util.function.Function;

public @interface Query {

    Class<Function<Map<String, Object>, DetachedQuery>> value();

}
