package com.agorapulse.micronaut.aws.kinesis

/**
 * Testing event.
 */
class MyEvent extends DefaultEvent {

    // java way
    MyEvent(String value) {
        this.value = value
    }

    // groovy way
    MyEvent() { }

    String value
}
