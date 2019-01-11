package com.agorapulse.micronaut.aws.ses;

public enum EmailDeliveryStatus {

    STATUS_DELIVERED,
    STATUS_BLACKLISTED,
    STATUS_NOT_DELIVERED;

    public final boolean asBoolean() {
        return STATUS_DELIVERED.equals(this);
    }
}
