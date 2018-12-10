package com.agorapulse.micronaut.aws.kinesis.annotation;

import com.agorapulse.micronaut.aws.kinesis.KinesisClientIntroduction;
import groovy.transform.Undefined;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.*;

@Introduction
@Type(KinesisClientIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface KinesisClient {

    /**
     * @return the name of the configuration to use.
     */
    String value() default "default";

    /**
     * @return the default name of the string overriding the one from the configuration
     */
    String stream() default Undefined.STRING;

    final class Constants {
        public static final String STREAM = "stream";
    }

}
