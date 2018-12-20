package com.agorapulse.micronaut.aws.kinesis;

class MyEvent extends AbstractEvent {

    // java way
    MyEvent(String value) {
        this.value = value
    }

    // groovy way
    MyEvent() {}

    String value
}
