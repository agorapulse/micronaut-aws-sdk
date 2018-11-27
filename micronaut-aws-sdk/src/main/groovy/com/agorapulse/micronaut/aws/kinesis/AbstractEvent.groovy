package com.agorapulse.micronaut.aws.kinesis

abstract class AbstractEvent implements Event {

    Date timestamp = new Date()
    String consumerFilterKey = '' // Ex.: 'ben', 'flo' (to share Streams between different environment, for example devs)

}
