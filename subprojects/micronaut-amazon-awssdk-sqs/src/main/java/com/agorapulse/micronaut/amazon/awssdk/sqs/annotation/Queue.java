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
package com.agorapulse.micronaut.amazon.awssdk.sqs.annotation;

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
