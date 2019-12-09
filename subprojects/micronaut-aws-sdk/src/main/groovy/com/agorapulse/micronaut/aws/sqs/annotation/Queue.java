package com.agorapulse.micronaut.aws.sqs.annotation;

import groovy.transform.Undefined;

import java.lang.annotation.*;


/**
 * Declares the name of the queue to publish messages.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface Queue {

    /**
     * @return the name of the queue to publish new messages.
     */
    String value();


    /**
     * @return default delay for the messages published
     */
    int delay() default 0;

    /**
     * @return the default message group id for fifo queues
     */
    String group() default Undefined.STRING;

}
