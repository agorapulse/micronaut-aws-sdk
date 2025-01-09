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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service(DynamoDBEntityNoRange.class)
public interface DynamoDBEntityNoRangeService {

    DynamoDBEntityNoRange get(@PartitionKey String parentId);
    List<DynamoDBEntityNoRange> getAll(@PartitionKey List<String> parentIds);
    DynamoDBEntityNoRange save(DynamoDBEntityNoRange entity);
    Flowable<DynamoDBEntityNoRange> saveAll(DynamoDBEntityNoRange... entities);

    Flowable<DynamoDBEntityNoRange> saveAll(Flowable<DynamoDBEntityNoRange> entities);

    Flux<DynamoDBEntityNoRange> saveAll(Iterable<DynamoDBEntityNoRange> entities);

    int count(String hashKey);

    List<DynamoDBEntityNoRange> query(String hashKey);

    void delete(DynamoDBEntityNoRange entity);

    void deleteByPartition(@PartitionKey String partitionKey);

    class ByHash implements Function<Map<String, Object>, DetachedQuery> {
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntityNoRange.class)
                .partitionKey(arguments.get("hashKey"));
        }
    }
    @Query(ByHash.class)
    int deleteByHash(String hashKey);

    class IncrementNumber implements Function<Map<String, Object>, DetachedUpdate> {
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntityNoRange.class)
                .partitionKey(arguments.get("hashKey"))
                .add("number", 1)
                .returnUpdatedNew(DynamoDBEntityNoRange::getNumber);
        }
    }
    @Update(IncrementNumber.class)
    Number increment(String hashKey);

    class DecrementNumber implements Function<Map<String, Object>, DetachedUpdate> {
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntityNoRange.class)
                .partitionKey(arguments.get("hashKey"))
                .add("number", -1)
                .returnUpdatedNew(DynamoDBEntityNoRange::getNumber);
        }
    }
    @Update(DecrementNumber.class)
    Number decrement(String hashKey);

    class DeleteNumber implements Function<Map<String, Object>, DetachedUpdate> {
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntityNoRange.class)
                .partitionKey(arguments.get("hashKey"))
                .delete("number");
        }
    }
    @Update(DeleteNumber.class)
    void deleteNumber(String hashKey);

}
