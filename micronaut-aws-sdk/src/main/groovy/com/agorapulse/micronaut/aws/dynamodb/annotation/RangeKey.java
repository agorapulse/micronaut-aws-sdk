package com.agorapulse.micronaut.aws.dynamodb.annotation;

import java.lang.annotation.*;

/**
 * Annotates range key.
 *
 * This annotation is not required if the name of the argument contains word <code>range</code>.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface RangeKey { }
