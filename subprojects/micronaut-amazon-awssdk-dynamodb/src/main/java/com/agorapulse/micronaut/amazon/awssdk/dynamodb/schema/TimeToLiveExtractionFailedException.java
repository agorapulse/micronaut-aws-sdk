/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import io.micronaut.core.beans.BeanProperty;

public class TimeToLiveExtractionFailedException extends IllegalStateException {

    private final transient Object instance;
    private final transient BeanProperty<?, ?> property;

    public TimeToLiveExtractionFailedException(Object instance, BeanProperty<?, ?> property, String message, Throwable cause) {
        super(message, cause);
        this.instance = instance;
        this.property = property;
    }

    @SuppressWarnings("java:S1452")
    public BeanProperty<?, ?> getProperty() {
        return property;
    }

    public Object getInstance() {
        return instance;
    }

}
