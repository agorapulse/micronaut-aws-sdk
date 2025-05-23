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
package com.agorapulse.micronaut.aws.ses

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.agorapulse.micronaut.aws.util.ConfigurationUtil
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires

import jakarta.inject.Named

/**
 * Default simple storage service configuration.
 */
@Primary
@CompileStatic
@SuppressWarnings('OptionalField')
@ConfigurationProperties('aws.ses')
@Requires(classes = AmazonSimpleEmailService)
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
class SimpleEmailServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    Optional<String> sourceEmail = Optional.empty()
    Optional<String> subjectPrefix = Optional.empty()
    Optional<Boolean> useBase64EncodingForMultipartEmails = Optional.empty()

}
