/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.aws.sqs.annotation;


import com.agorapulse.micronaut.aws.sqs.QueueClientIntroduction;
import com.agorapulse.micronaut.aws.util.ConfigurationUtil;
import groovy.transform.Undefined;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.*;

@Introduction
@Type(QueueClientIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface QueueClient {

    /**
     * @return the name of the configuration to use.
     */
    String value() default ConfigurationUtil.DEFAULT_CONFIGURATION_NAME;

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

