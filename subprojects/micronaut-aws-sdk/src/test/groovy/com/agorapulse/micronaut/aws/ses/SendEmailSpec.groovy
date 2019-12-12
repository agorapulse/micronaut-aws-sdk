/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest
import com.amazonaws.services.simpleemail.model.SendRawEmailResult
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject

/**
 * Tests for sending emails with Groovy.
 */
class SendEmailSpec extends Specification {

    AmazonSimpleEmailService simpleEmailService = Mock(AmazonSimpleEmailService)

    @Rule
    TemporaryFolder tmp = new TemporaryFolder()

    @Subject
    SimpleEmailService service = new DefaultSimpleEmailService(simpleEmailService)

    void "send email"() {
        given:
            File file = tmp.newFile('test.pdf')
            file.text = 'not a real PDF'
            String thePath = file.canonicalPath
        when:
            EmailDeliveryStatus status = service.send {                                 // <1>
                subject 'Hi Paul'                                                       // <2>
                from 'subscribe@groovycalamari.com'                                     // <3>
                to 'me@sergiodelamo.com'                                                // <4>
                htmlBody '<p>This is an example body</p>'                               // <5>
                attachment {                                                            // <6>
                    filepath thePath                                                    // <7>
                    filename 'test.pdf'                                                 // <8>
                    mimeType 'application/pdf'                                          // <9>
                    description 'An example pdf'                                        // <10>
                }
            }

        then:
            status == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendRawEmail(_) >> { SendRawEmailRequest request ->
                return new SendRawEmailResult().withMessageId('foobar')
            }
    }
}
