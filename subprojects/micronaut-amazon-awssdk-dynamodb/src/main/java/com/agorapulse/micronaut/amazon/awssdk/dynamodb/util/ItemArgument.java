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

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.Argument;
import org.reactivestreams.Publisher;

import java.util.Optional;

public class ItemArgument {

    public static <T> Optional<ItemArgument> findItemArgument(Class<T> itemType, MethodInvocationContext<Object, Object> context) {
        Argument<?>[] args = context.getArguments();

        if (args.length == 1) {
            Argument<?> itemArgument = args[0];
            if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
                ItemArgument item = new ItemArgument();
                item.argument = itemArgument;
                item.single = false;
                return Optional.of(item);
            }

            if (itemType.isAssignableFrom(itemArgument.getType())) {
                ItemArgument item = new ItemArgument();
                item.argument = itemArgument;
                item.single = true;
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }


    Argument<?> argument;
    boolean single;

    public Argument<?> getArgument() {
        return argument;
    }

    public boolean isSingle() {
        return single;
    }
}
