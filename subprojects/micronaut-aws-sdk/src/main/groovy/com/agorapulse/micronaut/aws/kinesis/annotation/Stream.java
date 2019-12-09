package com.agorapulse.micronaut.aws.kinesis.annotation;

import java.lang.annotation.*;


/**
 * Declares the name of the stream to publish events.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface Stream {

    /**
     * @return the name of the stream to publish new records.
     */
    String value();

}
