/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.ses

import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import spock.lang.Specification
import spock.lang.Subject

/**
 * Tests for simple email service.
 */
@SuppressWarnings(['AbcMetric', 'JavaIoPackageAccess', 'UnnecessaryObjectReferences'])
class SimpleEmailServiceSpec extends Specification {

    SesClient simpleEmailService = Mock(SesClient)

    @Subject
    SimpleEmailService service = new DefaultSimpleEmailService(
        simpleEmailService,
        new SimpleEmailServiceConfiguration(
            sourceEmail: Optional.of('Vladimir Orany <vlad@agorapulse.com>'),
            subjectPrefix: Optional.of('[TEST]')
        )
    )

    void "test transactionalEmailWithClosure"() {
        given:
        Map<String, String> customTags = [key1: 'value1', key2: 'value2']
        when:
        TransactionalEmail transactionalEmail = SimpleEmailService.email {
            subject 'Hi Paul'
            from 'subscribe@groovycalamari.com'
            to 'me@sergiodelamo.com'
            htmlBody '<p>This is an example body</p>'
            configurationSetName 'configuration-set'
            tags customTags
            attachment {
                filename 'test.pdf'
                filepath '/tmp/test.pdf'
                mimeType 'application/pdf'
                description 'An example pdf'
            }
        }

        then:
        transactionalEmail
        transactionalEmail.subject == 'Hi Paul'
        transactionalEmail.htmlBody == '<p>This is an example body</p>'
        transactionalEmail.from == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.configurationSetName == 'configuration-set'
        transactionalEmail.tags == [key1: 'value1', key2: 'value2']
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'test.pdf'
        transactionalEmail.attachments.first().filepath == '/tmp/test.pdf'
        transactionalEmail.attachments.first().mimeType == 'application/pdf'
        transactionalEmail.attachments.first().description == 'An example pdf'

        when:
        File f = new File(SimpleEmailServiceSpec.getResource('groovylogo.png').file)

        then:
        f.exists()

        when:
        transactionalEmail = SimpleEmailService.email {
            subject 'Hi Paul'
            htmlBody '<p>This is an example body</p>'
            to 'me@sergiodelamo.com'
            from 'subscribe@groovycalamari.com'
            configurationSetName 'configuration-set'
            tags customTags
            attachment {
                filepath f.absolutePath
            }
        }

        then:
        transactionalEmail
        transactionalEmail.subject == 'Hi Paul'
        transactionalEmail.htmlBody == '<p>This is an example body</p>'
        transactionalEmail.from == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.configurationSetName == 'configuration-set'
        transactionalEmail.tags == [key1: 'value1', key2: 'value2']
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'groovylogo.png'
        transactionalEmail.attachments.first().filepath == f.absolutePath
        transactionalEmail.attachments.first().mimeType == 'image/png'
        transactionalEmail.attachments.first().description == ''
    }

    void 'transaction email must have at least one recepient'() {
        when:
            SimpleEmailService.email {
                from 'vladimir@orany.cz'
            }
        then:
            thrown(IllegalArgumentException)
    }

    void "test that if you try to send an unsupported attachment an exception is thrown "() {
        when:
            service.send {
                subject 'GROOVY AWS SDK SES with Attachment'
                htmlBody '<p>This is an example body</p>'
                to 'test.to@example.com'
                from 'test.from@example.com'
                attachment {
                    filepath '/temp/virus.exe'
                    filename 'virus.exe'
                    mimeType 'application/octet-stream'
                }
            }

        then:
        thrown UnsupportedAttachmentTypeException
    }

    void 'exe is not supported'() {
        expect:
            !MimeType.isFileExtensionSupported('virus.exe')
    }

    void "test send method delivers an email"() {
        when:
            EmailDeliveryStatus deliveryIndicator = service.send {
                to 'test.to@example.com'
                subject 'Groovy AWS SDK SES Subject'
                replyTo 'test.reply@example.com'
            }
        then:
            deliveryIndicator == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendEmail(_) >> { SendEmailRequest request ->
                return SendEmailResponse.builder().messageId('foobar').build()
            }
    }

    void "test send method handles blacklisted address"() {
        when:
            EmailDeliveryStatus deliveryIndicator = service.send {
                to 'test.to@example.com'
                subject 'Groovy AWS SDK SES Subject'
                replyTo 'test.reply@example.com'
            }
        then:
            deliveryIndicator == EmailDeliveryStatus.STATUS_BLACKLISTED

            simpleEmailService.sendEmail(_) >> { SendEmailRequest request ->
                request.source() == 'Vladimir Orany <vlad@agorapulse.com>'
                request.message().subject().data().startsWith('[TEST] ')
                throw AwsServiceException.builder().message('Address blacklisted').build()
            }
    }

    void "test send method handles exceptions"() {
        when:
            EmailDeliveryStatus deliveryIndicator = service.send {
                to 'test.to@example.com'
                subject 'Groovy AWS SDK SES Subject'
                from 'test.from@example.com'
                replyTo 'test.reply@example.com'
            }
        then:
            deliveryIndicator == EmailDeliveryStatus.STATUS_NOT_DELIVERED

            simpleEmailService.sendEmail(_) >> { SendEmailRequest request ->
                throw AwsServiceException.builder().message('Generic exception').build()
            }
    }

    void "test send attachment"() {
        when:
            File f = new File(SimpleEmailServiceSpec.getResource('groovylogo.png').file)

        then:
            f.exists()

        when:
            String subjectStr = 'GRAILS AWS SDK SES with Attachment'
            TransactionalEmail email = SimpleEmailService.email {
                subject subjectStr
                htmlBody '<p>This is an example body</p>'
                to 'test.to@example.com'
                attachment {
                    filepath f.absolutePath
                }
            }
            EmailDeliveryStatus deliveryIndicator = service.send email
        then:
            email.attachments.first().toString().contains('filename=\'groovylogo.png\'')
            email.attachments.first().toString().contains('mimeType=\'image/png\'')

            deliveryIndicator == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendRawEmail(_) >> { SendRawEmailRequest request ->
                request.source() == 'Vladimir Orany <vlad@agorapulse.com>'
                return SendRawEmailResponse.builder().messageId('foobar').build()
            }
    }

    void "test send with wrong attachment"() {
        when:
            File f = new File('no-such-file.png')
            String subjectStr = 'GRAILS AWS SDK SES with Attachment'
            EmailDeliveryStatus deliveryIndicator = service.send {
                subject subjectStr
                htmlBody '<p>This is an example body</p>'
                to 'test.to@example.com'
                attachment {
                    filename f.name
                    filepath f.absolutePath
                }
            }
        then:
            !deliveryIndicator

            simpleEmailService.sendRawEmail(_) >> { SendRawEmailRequest request ->
                return SendRawEmailResponse.builder().messageId('foobar').build()
            }
    }

}
