package com.agorapulse.micronaut.aws.kinesis.annotation;

import java.lang.annotation.*;

/**
 * Annotates sequence number for ordering. The value is always converted to String.
 *
 * This annotation is not required if the name of the argument contains word <code>sequence</code>.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface SequenceNumber {
}
