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
package com.agorapulse.micronaut.aws.ses;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for sending emails with Java.
 */
public class SendEmailTest {

    @TempDir public File tmp;

    private AmazonSimpleEmailService simpleEmailService = mock(AmazonSimpleEmailService.class);

    private SimpleEmailService service = new DefaultSimpleEmailService(
        simpleEmailService,
        new SimpleEmailServiceConfiguration()
    );

    @Test
    public void testSendEmail() throws IOException {
        when(simpleEmailService.sendRawEmail(Mockito.any()))
            .thenReturn(new SendRawEmailResult().withMessageId("foobar"));

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
                .attachment(a ->                                                        // <6>
                    a.filepath(filepath)                                                // <7>
                        .filename("test.pdf")                                           // <8>
                        .mimeType("application/pdf")                                    // <9>
                        .description("An example pdf")                                  // <10>
                )
        );
        // end::builder[]

        Assertions.assertEquals(EmailDeliveryStatus.STATUS_DELIVERED, status);
    }
}
