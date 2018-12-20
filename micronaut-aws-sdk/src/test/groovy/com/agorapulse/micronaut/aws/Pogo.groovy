package com.agorapulse.micronaut.aws

import groovy.transform.ToString

@ToString
class Pogo {

    // java way
    Pogo(String foo) {
        this.foo = foo
    }

    // groovy way
    Pogo() {}

    String foo
}
