/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.events;

public abstract class DynamoDbEvent<T> {

    public static <T> DynamoDbPostLoadEvent<T> postLoad(T entity) {
        return new DynamoDbPostLoadEvent<>(entity);
    }

    public static <T> DynamoDbPrePersistEvent<T> prePersist(T entity) {
        return new DynamoDbPrePersistEvent<>(entity);
    }

    public static <T> DynamoDbPostPersistEvent<T> postPersist(T entity) {
        return new DynamoDbPostPersistEvent<>(entity);
    }

    public static <T> DynamoDbPreRemoveEvent<T> preRemove(T entity) {
        return new DynamoDbPreRemoveEvent<>(entity);
    }

    public static <T> DynamoDbPostRemoveEvent<T> postRemove(T entity) {
        return new DynamoDbPostRemoveEvent<>(entity);
    }

    public static <T> DynamoDbPreUpdateEvent<T> preUpdate(T entity) {
        return new DynamoDbPreUpdateEvent<>(entity);
    }

    public static <T> DynamoDbPostUpdateEvent<T> postUpdate(T entity) {
        return new DynamoDbPostUpdateEvent<>(entity);
    }


    private final DynamoDbEventType type;
    private final T entity;

    protected DynamoDbEvent(DynamoDbEventType type, T entity) {
        this.type = type;
        this.entity = entity;
    }

    public DynamoDbEventType getType() {
        return type;
    }

    public T getEntity() {
        return entity;
    }
}
