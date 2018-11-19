package com.agorapulse.micronaut.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import com.amazonaws.services.simpleemail.model.SendEmailResult
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest
import com.amazonaws.services.simpleemail.model.SendRawEmailResult
import spock.lang.Specification
import spock.lang.Subject

@SuppressWarnings(['AbcMetric', 'JavaIoPackageAccess'])
class SimpleEmailServiceSpec extends Specification {

    AmazonSimpleEmailService simpleEmailService = Mock(AmazonSimpleEmailService)

    @Subject
    SimpleEmailService awsSesMailer = new SimpleEmailService(simpleEmailService)

    void "test transactionalEmailWithClosure"() {
        when:
        TransactionalEmail transactionalEmail = SimpleEmailService.transactionalEmailWithClosure {
            subject 'Hi Paul'
            htmlBody '<p>This is an example body</p>'
            to 'me@sergiodelamo.com'
            from 'subscribe@groovycalamari.com'
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
        transactionalEmail.sourceEmail == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.destinationEmail == 'me@sergiodelamo.com'
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'test.pdf'
        transactionalEmail.attachments.first().filepath == '/tmp/test.pdf'
        transactionalEmail.attachments.first().mimeType == 'application/pdf'
        transactionalEmail.attachments.first().description == 'An example pdf'

        when:
         
        File f = new File(SimpleEmailServiceSpec.class.getResource("groovylogo.png").getFile())

        then:
        f.exists()

        when:
        transactionalEmail = SimpleEmailService.transactionalEmailWithClosure {
            subject 'Hi Paul'
            htmlBody '<p>This is an example body</p>'
            to 'me@sergiodelamo.com'
            from 'subscribe@groovycalamari.com'
            attachment {
                filepath f.absolutePath
            }
        }

        then:
        transactionalEmail
        transactionalEmail.subject == 'Hi Paul'
        transactionalEmail.htmlBody == '<p>This is an example body</p>'
        transactionalEmail.sourceEmail == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.destinationEmail == 'me@sergiodelamo.com'
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'groovylogo.png'
        transactionalEmail.attachments.first().filepath == f.absolutePath
        transactionalEmail.attachments.first().mimeType == 'image/png'
        transactionalEmail.attachments.first().description == ''
    }

    void "test that if you try to send an unsupported attachment an exception is thrown "() {
        when:
        String subjectStr = 'GROOVY AWS SDK SES with Attachment'

        awsSesMailer.mailWithAttachment {
            subject subjectStr
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

    void "test AmazonSESService.mail method delivers an email"() {
        when:
            String subjectStr = 'Groovy AWS SDK SES Subject'
            EmailDeliveryStatus deliveryIndicator = awsSesMailer.mail {
                to 'test.to@example.com'
                subject subjectStr
                from 'test.from@example.com'
            }
        then:
            deliveryIndicator == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendEmail(_) >> { SendEmailRequest request ->
                return new SendEmailResult().withMessageId('foobar')
            }
    }

    void "test send attachment"() {
        when:
            File f = new File(SimpleEmailServiceSpec.class.getResource("groovylogo.png").getFile())

        then:
            f.exists()

        when:
            String subjectStr = 'GRAILS AWS SDK SES with Attachment'
            EmailDeliveryStatus deliveryIndicator = awsSesMailer.mailWithAttachment {
                subject subjectStr
                htmlBody '<p>This is an example body</p>'
                to 'test.to@example.com'
                from 'test.from@example.com'
                attachment {
                    filepath f.absolutePath
                }
            }
        then:
            deliveryIndicator == EmailDeliveryStatus.STATUS_DELIVERED

            simpleEmailService.sendRawEmail(_) >> { SendRawEmailRequest request ->
                return new SendRawEmailResult().withMessageId('foobar')
            }
    }
}
