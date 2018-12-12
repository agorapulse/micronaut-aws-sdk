package com.agorapulse.micronaut.aws.dynamodb.annotation;

import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedUpdate;

import java.lang.annotation.*;
import java.util.Map;
import java.util.function.Function;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Update {

    Class<? extends Function<Map<String, Object>, DetachedUpdate>> value();

}
