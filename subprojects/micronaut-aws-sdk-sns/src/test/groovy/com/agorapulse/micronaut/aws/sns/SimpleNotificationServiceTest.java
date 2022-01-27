/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.Topic;
import io.micronaut.context.ApplicationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.agorapulse.micronaut.aws.sns.SimpleNotificationService.PlatformType.GCM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

public class SimpleNotificationServiceTest {

    private static final String TEST_TOPIC = "TestTopic";
    private static final String EMAIL = "vlad@agorapulse.com";
    private static final String DEVICE_TOKEN = "DEVICE-TOKEN";
    private static final String API_KEY = "API-KEY";
    private static final String DATA = "Vlad";
    private static final String PHONE_NUMBER = "+420999888777";

    // tag::testcontainers-setup[]
    public ApplicationContext context;                                                  // <1>

    public SimpleNotificationService service;

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()                   // <2>
        .withServices(SNS);

    @Before
    public void setup() {
        AmazonSNS amazonSNS = AmazonSNSClient                                           // <3>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(SNS))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();


        Map<String, Object> properties = new HashMap<>();                               // <4>
        properties.put("aws.sns.topic", TEST_TOPIC);


        context = ApplicationContext.builder(properties).build();                       // <5>
        context.registerSingleton(AmazonSNS.class, amazonSNS);
        context.start();

        service = context.getBean(SimpleNotificationService.class);
    }

    @After
    public void cleanup() {
        if (context != null) {
            context.close();                                                            // <6>
        }
    }
    // end::testcontainers-setup[]

    @Test
    public void testWorkingWithTopics() {
        // tag::new-topic[]
        String topicArn = service.createTopic(TEST_TOPIC);                              // <1>

        Topic found = service.listTopics().filter(t ->                                  // <2>
            t.getTopicArn().endsWith(TEST_TOPIC)
        ).blockingFirst();
        // end::new-topic[]

        assertNotNull(found);

        // CHECKSTYLE:OFF
        // tag::subscription[]
        String subArn = service.subscribeTopicWithEmail(topicArn, EMAIL);               // <1>

        String messageId = service.publishMessageToTopic(                               // <2>
            topicArn,
            "Test Email",
            "Hello World"
        );

        service.unsubscribeTopic(subArn);                                               // <3>
        // end::subscription[]
        // CHECKSTYLE:ON

        assertNotNull(subArn);
        assertNotNull(messageId);

        // tag::delete-topic[]
        service.deleteTopic(topicArn);                                                  // <1>

        Long zero = service.listTopics().filter(t ->                                    // <2>
            t.getTopicArn().endsWith(TEST_TOPIC)
        ).count().blockingGet();
        // end::delete-topic[]

        assertEquals(Long.valueOf(0), zero);
    }

    @Test
    public void testPlatformApplications() {
        //CHECKSTYLE:OFF
        // tag::applications[]
        String appArn = service.createPlatformApplication(                              // <1>
            "my-app",
            GCM,
            null,
            API_KEY
        );

        String endpoint = service.createPlatformEndpoint(appArn, DEVICE_TOKEN, DATA);   // <2>

        String jsonMessage = "{\"data\", \"{\"foo\": \"some bar\"}\", \"notification\", \"{\"title\": \"some title\", \"body\": \"some body\"}\"}";

        String msgId = service.sendNotification(endpoint, GCM, jsonMessage);            // <3>

        service.validateDeviceToken(appArn, endpoint, DEVICE_TOKEN, DATA);              // <4>

        service.unregisterDevice(endpoint);                                             // <5>
        // end::applications[]
        //CHECKSTYLE:ON

        assertNotNull(appArn);
        assertNotNull(endpoint);
        assertNotNull(msgId);
    }

    @Test
    public void sendSMS() {
        // tag::sms[]
        Map<Object, Object> attrs = Collections.emptyMap();
        String msgId = service.sendSMSMessage(PHONE_NUMBER, "Hello World", attrs);      // <1>
        // end::sms[]

        assertNotNull(msgId);
    }

}
