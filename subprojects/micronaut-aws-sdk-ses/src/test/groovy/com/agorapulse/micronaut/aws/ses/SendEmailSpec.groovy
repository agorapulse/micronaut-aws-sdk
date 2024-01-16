/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.TempDir

/**
 * Tests for sending emails with Groovy.
 */
class SendEmailSpec extends Specification {

    AmazonSimpleEmailService simpleEmailService = Mock(AmazonSimpleEmailService)

    @TempDir
    File tmp

    @Subject
    SimpleEmailService service = new DefaultSimpleEmailService(
        simpleEmailService,
        new SimpleEmailServiceConfiguration()
    )

    void "send email"() {
        given:
            File file = new File(tmp, 'test.pdf')
            file.createNewFile()
            file.text = 'not a real PDF'
            String thePath = file.canonicalPath
            Map<String, String> mapOfTags = [myTagKey: 'myTagValue']
        when:
            // tag::builder[]
            EmailDeliveryStatus status = service.send {                                 // <1>
                subject 'Hi Paul'                                                       // <2>
                from 'subscribe@groovycalamari.com'                                     // <3>
                to 'me@sergiodelamo.com'                                                // <4>
                htmlBody '<p>This is an example body</p>'                               // <5>
                configurationSetName 'configuration-set'                                // <6>
                tags mapOfTags                                                          // <7>
                attachment {                                                            // <8>
                    filepath thePath                                                    // <9>
                    filename 'test.pdf'                                                 // <10>
                    mimeType 'application/pdf'                                          // <11>
                    description 'An example pdf'                                        // <12>
                }
            }
            // end::builder[]

        then:
            status == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendRawEmail(_) >> { SendRawEmailRequest request ->
                return new SendRawEmailResult().withMessageId('foobar')
            }
    }

}
