package com.agorapulse.micronaut.aws.sqs.annotation;


import com.agorapulse.micronaut.aws.sqs.QueueClientIntroduction;
import groovy.transform.Undefined;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Introduction
@Type(QueueClientIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface QueueClient {

    /**
     * @return the name of the configuration to use.
     */
    String value() default "default";

    /**
     * @return the default name of the queue overriding the one from the configuration
     */
    String queue() default Undefined.STRING;


    /**
     * @return default delay for the messages published
     */
    int delay() default 0;

    /**
     * @return the default message group id for fifo queues
     */
    String group() default Undefined.STRING;

    final class Constants {
        public static final String QUEUE = "queue";
        public static final String GROUP = "group";
        public static final String DELAY = "delay";
    }

}

