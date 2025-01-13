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
package com.agorapulse.micronaut.amazon.awssdk.core;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public interface RegionAndEndpointConfiguration {

    String getRegion();

    String getEndpoint();

    String getClient();

    String getAsyncClient();

    @Deprecated
    default <C, B extends AwsClientBuilder<B, C>> B configure(B builder, AwsRegionProvider awsRegionProvider) {
        return configure(builder, awsRegionProvider, null, Optional.empty());
    }

    default <C, B extends AwsClientBuilder<B, C>> B configure(B builder, AwsRegionProvider awsRegionProvider, ClientBuilderProvider builderProvider, Optional<SdkAsyncHttpClient> httpClient) {
        builder.region(Optional.ofNullable(getRegion()).map(Region::of).orElseGet(awsRegionProvider::getRegion));

        if (getEndpoint() != null) {
            try {
                builder.endpointOverride(new URI(getEndpoint()));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Endpoint configured for " + getClass() + " is not a valid URI!", e);
            }
        }

        if (builderProvider != null && getClient() != null && builder instanceof SdkSyncClientBuilder<?, ?> clientBuilder) {
            builderProvider.findHttpClientBuilder(getClient()).ifPresent(clientBuilder::httpClientBuilder);
        }

        if (builderProvider != null && getAsyncClient() != null && builder instanceof AwsAsyncClientBuilder<?, ?> clientBuilder) {
            builderProvider.findAsyncHttpClientBuilder(getClient()).ifPresent(clientBuilder::httpClientBuilder);
        } else if (httpClient.isPresent() && builder instanceof AwsAsyncClientBuilder<?, ?> clientBuilder) {
            clientBuilder.httpClient(httpClient.get());
        }

        return builder;
    }

    default <B extends SdkPresigner.Builder> B configure(B builder, AwsRegionProvider awsRegionProvider) {
        builder.region(Optional.ofNullable(getRegion()).map(Region::of).orElseGet(awsRegionProvider::getRegion));

        if (getEndpoint() != null) {
            try {
                builder.endpointOverride(new URI(getEndpoint()));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Endpoint configured for " + getClass() + " is not a valid URI!", e);
            }
        }

        return builder;
    }


}
