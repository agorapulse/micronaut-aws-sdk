/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.exception;


import java.util.List;

/**
 * Exception thrown when a batch request fails.
 * <p>
 *     The exception contains a list of unprocessed items - for save operation it is a list of items that were not saved and for delete operation it is a list of keys that were not deleted.
 * </p>
 * <p>
 *     This exception is not thrown from async implementation but the unprocessed items are saved or deleted individually instead.
 * </p>
 */
public class FailedBatchRequestException extends IllegalArgumentException {

    private final transient List<?> unprocessedItems;

    public FailedBatchRequestException(String s, List<?> unprocessedItems) {
        super(s);
        this.unprocessedItems = unprocessedItems;
    }

    public List<?> getUnprocessedItems() {
        return unprocessedItems;
    }

}
