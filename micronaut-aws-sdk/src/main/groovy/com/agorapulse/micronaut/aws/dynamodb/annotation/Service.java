package com.agorapulse.micronaut.aws.dynamodb.annotation;

import com.agorapulse.micronaut.aws.dynamodb.ServiceIntroduction;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.*;

@Introduction
@Type(ServiceIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Service {

    /**
     * @return the class of the dynamodb items
     */
    Class value();

}
