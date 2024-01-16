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
package com.agorapulse.micronaut.aws.s3

import com.agorapulse.micronaut.aws.util.ConfigurationUtil
import com.amazonaws.services.s3.AmazonS3
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Primary
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@CompileStatic
@ConfigurationProperties('aws.s3')
@Requires(classes = AmazonS3)
class DefaultSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

}
