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
package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Collections;
import java.util.List;

public class FluxListTopicHelper {


    static Flux<Topic> generateTopics(AmazonSNS client) {
        return Flux.generate(client::listTopics, (ListTopicsResult listTopicsResult, SynchronousSink<List<Topic>> topicEmitter) -> {
                topicEmitter.next(listTopicsResult.getTopics());

                if (listTopicsResult.getNextToken() != null) {
                    return client.listTopics(listTopicsResult.getNextToken());
                }

                topicEmitter.complete();

                ListTopicsResult empty = new ListTopicsResult();
                empty.setTopics(Collections.emptyList());
                return empty;
        }).flatMap(Flux::fromIterable);
    }

}
