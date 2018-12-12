package com.agorapulse.micronaut.aws.dynamodb.annotation;

import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedScan;

import java.lang.annotation.*;
import java.util.Map;
import java.util.function.Function;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Scan {

    Class<? extends Function<Map<String, Object>, DetachedScan>> value();

}
