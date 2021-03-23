/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;

/**
 * Converts {@link Date} from and to ISO {@link String}.
 *
 * You may consider changing the type of your attributes to {@link java.time.Instant}
 * which is supported out of the box.
 */
public class DateToStringAttributeConverter implements AttributeConverter<Date> {

    private static final InstantAsStringAttributeConverter INSTANT_CONVERTER = InstantAsStringAttributeConverter.create();

    @Override
    public EnhancedType<Date> type() {
        return EnhancedType.of(Date.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Date input) {
        return INSTANT_CONVERTER.transformFrom(input.toInstant());
    }

    @Override
    public Date transformTo(AttributeValue input) {
        return Date.from(INSTANT_CONVERTER.transformTo(input));
    }

}
