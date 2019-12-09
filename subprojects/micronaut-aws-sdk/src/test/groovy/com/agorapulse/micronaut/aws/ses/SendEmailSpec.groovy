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
