package com.agorapulse.micronaut.aws.dynamodb.intro

import com.agorapulse.micronaut.aws.dynamodb.annotation.Service

/**
 * Testing service.
 */
@Service(IntroProblemEntity2)
interface IntroProblemEntity2DBService {

    IntroProblemEntity2 load(String hash)

}
