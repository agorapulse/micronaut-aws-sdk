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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.util;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Filter;
import io.micronaut.core.type.Argument;

public class FilterArgument {

    private Argument<?> firstArgument;
    private Argument<?> secondArgument;
    private String name;
    private boolean required;
    private Filter.Operator operator;

    public static String getArgumentName(Argument<?> argument) {
        return argument.isAnnotationPresent(Filter.class)
            ? argument.getAnnotation(Filter.class).stringValue("name").orElse(argument.getName())
            : argument.getName();
    }

    FilterArgument fill(Argument<?> argument) {
        if (firstArgument == null) {
            fillFirstArgument(argument);
        } else {
            secondArgument = argument;
        }

        return this;
    }

    private void fillFirstArgument(Argument<?> argument) {
        name = getArgumentName(argument);
        firstArgument = argument;
        required = !argument.isNullable();
        operator = argument.isAnnotationPresent(Filter.class)
            ? argument.getAnnotation(Filter.class).enumValue("value", Filter.Operator.class).orElse(Filter.Operator.EQ)
            : Filter.Operator.EQ;
    }

    public Argument<?> getFirstArgument() {
        return firstArgument;
    }

    public Argument<?> getSecondArgument() {
        return secondArgument;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Filter.Operator getOperator() {
        return operator;
    }

}
