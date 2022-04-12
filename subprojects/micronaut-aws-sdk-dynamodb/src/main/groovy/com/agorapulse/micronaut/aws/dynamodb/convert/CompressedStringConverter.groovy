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
package com.agorapulse.micronaut.aws.dynamodb.convert

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@CompileStatic
class CompressedStringConverter implements DynamoDBTypeConverter<ByteBuffer, String> {

    @Override
    ByteBuffer convert(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        GZIPOutputStream gos = new GZIPOutputStream(baos)

        try {
            gos.write(input.getBytes(StandardCharsets.UTF_8))
            gos.close()

            byte[] compressedBytes = baos.toByteArray()

            ByteBuffer buffer = ByteBuffer.allocate(compressedBytes.length)
            buffer.put(compressedBytes, 0, compressedBytes.length)
            buffer.position(0)
            return buffer
        } finally {
            baos.close()
        }
    }

    @Override
    String unconvert(ByteBuffer input) {
        byte[] bytes = input.array()
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes)
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        GZIPInputStream is = new GZIPInputStream(bais)

        try {
            int chunkSize = 1024
            byte[] buffer = new byte[chunkSize]
            int length = 0
            while ((length = is.read(buffer, 0, chunkSize)) != -1) {
                baos.write(buffer, 0, length)
            }
            return new String(baos.toByteArray(), 'UTF-8')
        } finally {
            is.close()
            baos.close()
            bais.close()
        }
    }

}
