/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
