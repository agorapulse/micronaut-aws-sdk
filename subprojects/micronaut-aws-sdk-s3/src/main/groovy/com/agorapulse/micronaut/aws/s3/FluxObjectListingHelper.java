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
package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import reactor.core.publisher.Flux;

class FluxObjectListingHelper {

    static Flux<ObjectListing> generate(AmazonS3 client, String bucketName, String prefix) {
        return Flux.generate(
            () -> client.listObjects(bucketName, prefix),
            (objectListing, emitter) -> {
                emitter.next(objectListing);

                if (!objectListing.isTruncated()) {
                    emitter.complete();
                }

                return client.listNextBatchOfObjects(objectListing);
        });
    }
}
