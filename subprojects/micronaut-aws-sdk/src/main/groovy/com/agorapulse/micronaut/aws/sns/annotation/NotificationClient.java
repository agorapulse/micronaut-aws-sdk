package com.agorapulse.micronaut.aws.sns.annotation;

import com.agorapulse.micronaut.aws.sns.NotificationClientIntroduction;
import groovy.transform.Undefined;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.*;

@Introduction
@Type(NotificationClientIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface NotificationClient {

    /**
     * @return the name of the configuration to use for this client
     */
    String value() default "default";

    /**
     * @return default topic for this client which overrides the one from the configuration
     */
    String topic() default Undefined.STRING;

    class Constants {
        public static final String TOPIC = "topic";
    }

}
