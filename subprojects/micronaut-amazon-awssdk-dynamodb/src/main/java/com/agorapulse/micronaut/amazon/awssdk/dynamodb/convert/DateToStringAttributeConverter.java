package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TimeConversion;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Date;

/**
 * Converts {@link Date} from and to ISO {@link String}.
 *
 * You may consider changing the type of your attributes to {@link java.time.Instant}
 * which is supported out of the box.
 */
public class DateToStringAttributeConverter implements AttributeConverter<Date> {

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
        return TimeConversion.toStringAttributeValue(input.toInstant());
    }

    @Override
    public Date transformTo(AttributeValue input) {
        return Date.from(TimeConversion.instantFromAttributeValue(EnhancedAttributeValue.fromAttributeValue(input)));
    }

}
