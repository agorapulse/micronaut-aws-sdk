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
package com.agorapulse.micronaut.amazon.awssdk.ses;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties("aws.ses")
public class SimpleEmailServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    private Optional<String> sourceEmail = Optional.empty();
    private Optional<String> subjectPrefix = Optional.empty();

    public Optional<String> getSourceEmail() {
        return sourceEmail;
    }

    public void setSourceEmail(Optional<String> sourceEmail) {
        this.sourceEmail = sourceEmail;
    }

    public Optional<String> getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(Optional<String> subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

}
