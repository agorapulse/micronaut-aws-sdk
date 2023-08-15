/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.ses;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for sending emails with Java.
 */
public class SendEmailTest {

    @TempDir public File tmp;

    private SesClient simpleEmailService = mock(SesClient.class);

    private SimpleEmailService service = new DefaultSimpleEmailService(simpleEmailService, new SimpleEmailServiceConfiguration());

    @Test
    public void testSendEmail() throws IOException {
        when(simpleEmailService.sendRawEmail(Mockito.any(SendRawEmailRequest.class)))
            .thenReturn(SendRawEmailResponse.builder().messageId("foobar").build());

        File file = new File(tmp, "test.pdf");
        file.createNewFile();

        Files.write(file.toPath(), Collections.singletonList("not a real PDF"));
        String filepath = file.getCanonicalPath();

        // tag::builder[]
        EmailDeliveryStatus status = service.send(e ->                                  // <1>
            e.subject("Hi Paul")                                                        // <2>
                .from("subscribe@groovycalamari.com")                                   // <3>
                .to("me@sergiodelamo.com")                                              // <4>
                .htmlBody("<p>This is an example body</p>")                             // <5>
                .tags(Map.of("myTagKey", "myTagValue"))                         // <6>
                .attachment(a ->                                                        // <7>
                    a.filepath(filepath)                                                // <8>
                        .filename("test.pdf")                                           // <9>
                        .mimeType("application/pdf")                                    // <10>
                        .description("An example pdf")                                  // <11>
                )
        );
        // end::builder[]

        Assertions.assertEquals(EmailDeliveryStatus.STATUS_DELIVERED, status);
    }
}
