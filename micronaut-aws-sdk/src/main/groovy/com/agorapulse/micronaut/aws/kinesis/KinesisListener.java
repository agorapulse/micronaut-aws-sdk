package com.agorapulse.micronaut.aws.kinesis;

import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Parallel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Executable(processOnStartup = true)
@Parallel
public @interface KinesisListener {

    /**
     * @return the name of the client configuration
     */
    String value() default "default";

}
