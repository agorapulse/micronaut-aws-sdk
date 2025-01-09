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
package com.agorapulse.micronaut.amazon.awssdk.cloudwatchlogs;

import io.micronaut.retry.annotation.Retryable;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class DefaultCloudWatchLogsService implements CloudWatchLogsService {

    private final CloudWatchLogsClient cloudWatchLogsClient;

    public DefaultCloudWatchLogsService(CloudWatchLogsClient cloudWatchLogsClient) {
        this.cloudWatchLogsClient = cloudWatchLogsClient;
    }

    @Override
    @Retryable(includes = ResourceNotFoundException.class)
    public Stream<OutputLogEvent> getLogEvents(String logGroup) {
        List<LogStream> logStreams = cloudWatchLogsClient
            .describeLogStreams(streams -> streams.logGroupName(logGroup))
            .logStreams();

        if (logStreams == null) {
            return Stream.empty();
        }

        return logStreams
            .stream()
            .map(stream -> cloudWatchLogsClient.getLogEvents(events -> events.logStreamName(stream.logStreamName()).logGroupName(logGroup)))
            .flatMap(events -> {
                List<OutputLogEvent> logEvents = events.events();

                if (logEvents == null) {
                    return Stream.empty();
                }

                return logEvents.stream();
            });
    }
}
