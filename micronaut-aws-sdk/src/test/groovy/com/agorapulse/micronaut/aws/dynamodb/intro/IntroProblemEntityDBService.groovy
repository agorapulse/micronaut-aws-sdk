package com.agorapulse.micronaut.aws.dynamodb.intro

import com.agorapulse.micronaut.aws.dynamodb.annotation.Service

@Service(IntroProblemEntity)
interface IntroProblemEntityDBService {

    IntroProblemEntity load(String hash)

}