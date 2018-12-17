package com.agorapulse.micronaut.aws.dynamodb.annotation;

import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedQuery;

import java.lang.annotation.*;
import java.util.Map;
import java.util.function.Function;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Query {

    Class<? extends Function<Map<String, Object>, DetachedQuery>> value();

}
