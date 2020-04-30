package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation;

import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Projection type hint for generated indices.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Projection {

    ProjectionType value();

}
