package com.agorapulse.micronaut.aws.sns.annotation;

import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface Topic {

    /**
     * @return the topic for particular method call
     */
    String value();

}
