package com.agorapulse.micronaut.aws.kinesis

/**
 * Base for event classes.
 */
@SuppressWarnings('NoJavaUtilDate')
class DefaultEvent implements Event {

    Date timestamp = new Date()
    String consumerFilterKey = '' // Ex.: 'ben', 'flo' (to share Streams between different environment, for example devs)

}
