/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.apigateway.ws

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.stehno.ersatz.ContentType
import com.stehno.ersatz.ErsatzServer
import com.stehno.ersatz.RequestWithContent
import io.micronaut.context.ApplicationContext
import org.hamcrest.Matchers
import com.fasterxml.jackson.databind.JsonMappingException
import spock.lang.AutoCleanup
import spock.lang.Specification

import static groovy.lang.Closure.DELEGATE_FIRST

/**
 * Tests for MessageSender.
 */
class MessageSenderSpec extends Specification {

    private static final String ACCESS__KEY = 'ACCESS_KEY'
    private static final String SECRET__KEY = 'SECRET_KEY'
    private static final String CONNECTION_ID = 'connectionId'
    private static final Map<String, String> PAYLOAD = [foo: 'bar']

    @AutoCleanup
    ErsatzServer server = new ErsatzServer({
        reportToConsole()
    })

    @AutoCleanup
    ApplicationContext ctx

    void 'test posting to the url'() {
        given:
            prepareServerAndContext {
                header('Authorization', Matchers.iterableWithSize(1))
                called(1)
                responds().code(200)
            }
            MessageSender defaultSender = ctx.getBean(MessageSender)
        when:
            defaultSender.send(CONNECTION_ID, PAYLOAD)
        then:
            server.verify()
    }

    void 'test gone'() {
        given:
            prepareServerAndContext {
                header('Authorization', Matchers.iterableWithSize(1))
                called(1)
                responds().code(410).body('Already Gone', ContentType.TEXT_PLAIN)
            }
            MessageSender defaultSender = ctx.getBean(MessageSender)
        when:
            defaultSender.send(CONNECTION_ID, PAYLOAD)
        then:
            WebSocketClientGoneException e = thrown(WebSocketClientGoneException)
            e.connectionId == CONNECTION_ID
    }

    void 'test forbidden'() {
        given:
            prepareServerAndContext {
                header('Authorization', Matchers.iterableWithSize(1))
                called(1)
                responds().code(403).body('Forbidden', ContentType.TEXT_PLAIN)
            }
            MessageSender defaultSender = ctx.getBean(MessageSender)
        when:
            defaultSender.send(CONNECTION_ID, PAYLOAD)
        then:
            thrown(AmazonClientException)
    }

    void 'send unsupported payload'() {
        given:
            prepareServerAndContext {
                header('Authorization', Matchers.iterableWithSize(1))
                responds().code(200)
            }
            MessageSender defaultSender = ctx.getBean(MessageSender)
        when:
            defaultSender.send(CONNECTION_ID) { 'not supported' }
        then:
            IllegalArgumentException illegalArgumentException = thrown(IllegalArgumentException)
            illegalArgumentException.cause instanceof JsonMappingException
    }

    void prepareServerAndContext(@DelegatesTo(value = RequestWithContent, strategy = DELEGATE_FIRST) Closure response) {
        server.expectations {
            post("/$CONNECTION_ID/", response)
        }
        ctx = ApplicationContext.build(
            'aws.websocket.connections.url': server.httpUrl
        ).build()

        ctx.registerSingleton(AWSCredentialsProvider, new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS__KEY, SECRET__KEY)))

        ctx.start()
    }

}
