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
package com.agorapulse.micronaut.amazon.awssdk.s3

import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.multipart.PartData

import java.nio.ByteBuffer

@CompileStatic
class MockPartData implements PartData {

    private final String text

    MockPartData(String text) {
        this.text = text
    }

    @Override
    InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(text.bytes)
    }

    @Override
    byte[] getBytes() throws IOException {
        return text.bytes
    }

    @Override
    ByteBuffer getByteBuffer() throws IOException {
        throw new UnsupportedOperationException('Not implemented')
    }

    @Override
    Optional<MediaType> getContentType() {
        return Optional.of(MediaType.TEXT_PLAIN_TYPE)
    }

}
