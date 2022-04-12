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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CompressedStringConverter implements AttributeConverter<String> {

    @Override
    public AttributeValue transformFrom(String input) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(input.getBytes(StandardCharsets.UTF_8));
            gos.close();

            return AttributeValue.builder()
                .b(SdkBytes.fromByteArray(baos.toByteArray()))
                .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String transformTo(AttributeValue input) {
        byte[] bytes = input.b().asByteArray();

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPInputStream gzis = new GZIPInputStream(bais)) {

            byte[] buffer = new byte[1024];
            int len = 0;

            while ((len = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return baos.toString(StandardCharsets.UTF_8.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EnhancedType<String> type() {
        return EnhancedType.of(String.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.B;
    }

}
