package com.agorapulse.micronaut.aws.kinesis.annotation;

import java.lang.annotation.*;

/**
 * Annotates partition key. The value is always converted to String.
 *
 * This annotation is not required if the name of the argument contains word <code>key</code>.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PartitionKey { }
