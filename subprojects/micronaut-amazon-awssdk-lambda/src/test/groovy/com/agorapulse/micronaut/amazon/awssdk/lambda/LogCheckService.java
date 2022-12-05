/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.lambda;

import com.agorapulse.micronaut.amazon.awssdk.cloudwatchlogs.CloudWatchLogsService;

import javax.inject.Singleton;

@Singleton
public class LogCheckService {

    private final CloudWatchLogsService logsService;                                    // <1>

    public LogCheckService(CloudWatchLogsService logsService) {
        this.logsService = logsService;
    }

    public boolean contains(String logGroup, String text) {
        return logsService.getLogEvents(logGroup)
            .anyMatch(e -> e.message().contains(text));                                 // <2>
    }

}
