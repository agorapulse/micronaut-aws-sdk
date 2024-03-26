/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.aws.lambda.annotation;


import com.agorapulse.micronaut.aws.lambda.LambdaClientIntroduction;
import com.agorapulse.micronaut.aws.util.ConfigurationUtil;
import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;

import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Introduction
@Type(LambdaClientIntroduction.class)
@Scope
@Singleton
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface LambdaClient {

    /**
     * @return the name of the configuration to use.
     */
    String value() default ConfigurationUtil.DEFAULT_CONFIGURATION_NAME;

    /**
     * @return the default name of the function
     */
    String function() default "";

    final class Constants {

        public static final String FUNCTION = "function";

    }

}

