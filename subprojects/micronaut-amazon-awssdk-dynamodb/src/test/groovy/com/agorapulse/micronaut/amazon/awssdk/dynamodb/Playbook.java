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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.events.*;
import io.micronaut.context.event.ApplicationEventListener;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
class Playbook {

    private final List<String> entries = new ArrayList<>();

    public interface DynamoDbEntityEventListener<T extends DynamoDbEvent<? extends PlaybookAware>> extends ApplicationEventListener<T> {

        default boolean supports(T event) {
            return event.getEntity() instanceof DynamoDBEntity;
        }

    }

    @Singleton
    public static class PrePersist implements DynamoDbEntityEventListener<DynamoDbPrePersistEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PrePersist(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPrePersistEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    @Singleton
    public static class PostPersist implements DynamoDbEntityEventListener<DynamoDbPostPersistEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PostPersist(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPostPersistEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    @Singleton
    public static class PreRemove implements DynamoDbEntityEventListener<DynamoDbPreRemoveEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PreRemove(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPreRemoveEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    @Singleton
    public static class PostRemove implements DynamoDbEntityEventListener<DynamoDbPostRemoveEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PostRemove(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPostRemoveEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    @Singleton
    public static class PreUpdate implements DynamoDbEntityEventListener<DynamoDbPreUpdateEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PreUpdate(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPreUpdateEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    @Singleton
    public static class PostUpdate implements DynamoDbEntityEventListener<DynamoDbPostUpdateEvent<DynamoDBEntity>> {

        private final Playbook playbook;

        public PostUpdate(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPostUpdateEvent<DynamoDBEntity> event) {
            playbook.logEvent(event);
        }

    }

    // using interface test
    @Singleton
    public static class PostLoad implements DynamoDbEntityEventListener<DynamoDbPostLoadEvent<PlaybookAware>> {

        private final Playbook playbook;

        public PostLoad(Playbook playbook) {
            this.playbook = playbook;
        }

        public void onApplicationEvent(DynamoDbPostLoadEvent<PlaybookAware> event) {
            if (event.getEntity() instanceof DynamoDBEntity) {
                playbook.logEvent((DynamoDbPostLoadEvent) event);
            }
        }

    }

    boolean verifyAndForget(String... items) {
        System.out.println("Verifying playbook:");
        System.out.println("    Current items:");
        entries.forEach(it -> System.out.println("        " + it));
        System.out.println("    Expected items:");
        Arrays.stream(items).forEach(it -> System.out.println("        " + it));

        for (int i = 0; i < items.length; i++) {
            String expected = items[i];
            if (i < entries.size()) {
                String current = entries.get(i);
                if (!expected.equals(current)) {
                    throw new AssertionError("Expected " + expected + " but was " + current);
                }
            } else {
                throw new AssertionError("No recorded value found for " + expected);
            }

        }

        if (entries.size() != items.length) {
            throw new AssertionError("Different size of expected and recorded items!");
        }

        forget();

        return true;
    }

    boolean forget() {
        entries.clear();

        return true;
    }

    private void logEvent(DynamoDbEvent<DynamoDBEntity> event) {
        entries.add(
            String.format(
                "%s:%s:%s:%s:%s",
                event.getType(),
                event.getEntity().getParentId(),
                event.getEntity().getId(),
                event.getEntity().getRangeIndex(),
                event.getEntity().getNumber()
            )
        );
    }

}
