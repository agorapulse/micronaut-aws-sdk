/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.aws.cloudwatchlogs;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.*;
import io.micronaut.retry.annotation.Retryable;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class DefaultCloudWatchLogsService implements CloudWatchLogsService {

    private final AWSLogs cloudWatchLogsClient;

    public DefaultCloudWatchLogsService(AWSLogs cloudWatchLogsClient) {
        this.cloudWatchLogsClient = cloudWatchLogsClient;
    }

    @Override
    @Retryable(includes = ResourceNotFoundException.class)
    public Stream<OutputLogEvent> getLogEvents(String logGroup) {
        List<LogStream> logStreams = cloudWatchLogsClient
            .describeLogStreams(new DescribeLogStreamsRequest().withLogGroupName(logGroup))
            .getLogStreams();

        if (logStreams == null) {
            return Stream.empty();
        }

        return logStreams
            .stream()
            .map(stream -> cloudWatchLogsClient.getLogEvents(new GetLogEventsRequest().withLogStreamName(stream.getLogStreamName()).withLogGroupName(logGroup)))
            .flatMap(events -> {
                List<OutputLogEvent> logEvents = events.getEvents();

                if (logEvents == null) {
                    return Stream.empty();
                }

                return logEvents.stream();
            });
    }
}
