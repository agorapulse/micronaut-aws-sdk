package com.agorapulse.micronaut.aws.ses

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Requires

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.inject.Singleton
import javax.mail.BodyPart
import javax.mail.Session
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import java.nio.ByteBuffer

@Slf4j
@Singleton
@CompileStatic
@Requires(classes = AmazonSimpleEmailServiceClient.class)
class SimpleEmailService {

    private final AmazonSimpleEmailService client

    SimpleEmailService(AmazonSimpleEmailService client) {
        this.client = client
    }

    EmailDeliveryStatus mail(@DelegatesTo(value = TransactionalEmail, strategy = Closure.DELEGATE_FIRST) Closure composer) throws Exception {
        TransactionalEmail transactionalEmail = transactionalEmailWithClosure(composer)
        send(transactionalEmail.destinationEmail,
            transactionalEmail.subject,
            transactionalEmail.htmlBody,
            transactionalEmail.sourceEmail,
            transactionalEmail.replyToEmail)
    }

    EmailDeliveryStatus mailWithAttachment(@DelegatesTo(value = TransactionalEmail, strategy = Closure.DELEGATE_FIRST) Closure composer)
        throws UnsupportedAttachmentTypeException {
        TransactionalEmail transactionalEmail = transactionalEmailWithClosure(composer)
        sendEmailWithAttachment(transactionalEmail)
    }

    @SuppressWarnings(['LineLength', 'ElseBlockBraces', 'JavaIoPackageAccess'])
    EmailDeliveryStatus sendEmailWithAttachment(TransactionalEmail transactionalEmail) throws UnsupportedAttachmentTypeException {
        EmailDeliveryStatus status = EmailDeliveryStatus.STATUS_NOT_DELIVERED

        Session session = Session.getInstance(new Properties())
        MimeMessage mimeMessage = new MimeMessage(session)
        String subject = transactionalEmail.subject
        mimeMessage.setSubject(subject)
        MimeMultipart mimeMultipart = new MimeMultipart()

        BodyPart p = new MimeBodyPart()
        p.setContent(transactionalEmail.htmlBody, 'text/html')
        mimeMultipart.addBodyPart(p)

        for (TransactionalEmailAttachment attachment : transactionalEmail.attachments) {

            if (!MimeType.isMimeTypeSupported(attachment.mimeType)) {
                throw new UnsupportedAttachmentTypeException()
            }

            MimeBodyPart mimeBodyPart = new MimeBodyPart()
            mimeBodyPart.setFileName(attachment.filename)
            mimeBodyPart.setDescription(attachment.description, 'UTF-8')
            DataSource ds = new ByteArrayDataSource(new FileInputStream(new File(attachment.filepath)),
                attachment.mimeType)
            mimeBodyPart.setDataHandler(new DataHandler(ds))
            mimeMultipart.addBodyPart(mimeBodyPart)
        }
        mimeMessage.content = mimeMultipart

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()))

        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)

        rawEmailRequest.setDestinations(transactionalEmail.recipients)
        rawEmailRequest.setSource(transactionalEmail.sourceEmail)

        try {
            client.sendRawEmail(rawEmailRequest)
            status = EmailDeliveryStatus.STATUS_DELIVERED

        } catch (AmazonServiceException exception) {
            if (exception.message.find('Address blacklisted')) {

                log.debug "Address blacklisted destinationEmail=${transactionalEmail.recipients.toString()}"
                status = EmailDeliveryStatus.STATUS_BLACKLISTED

            } else if (exception.message.find('Missing final')) {
                log.warn "Invalid parameter value: destinationEmail=${transactionalEmail.recipients.toString()}, sourceEmail=${transactionalEmail.sourceEmail}, replyToEmail=${transactionalEmail.replyToEmail}, subject=${subject}"

            } else {
                log.warn 'An amazon service exception was catched while sending email with attachment' + exception.message
            }

        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was catched while sending email with attachment' + exception.message

        }
        status
    }

    /**
     *
     * @param destinationEmail
     * @param subject
     * @param htmlBody
     * @param sourceEmail
     * @param replyToEmail
     * @return 1 if successful, 0 if not sent, -1 if blacklisted
     */
    @SuppressWarnings(['LineLength', 'ElseBlockBraces'])
    EmailDeliveryStatus send(List<String> destinationEmails,
                             String subject,
                             String htmlBody,
                             String sourceEmail = '',
                             String replyToEmail = '') {
        EmailDeliveryStatus status = EmailDeliveryStatus.STATUS_NOT_DELIVERED

        if (!destinationEmails) {
            return status
        }

        Destination destination = new Destination(destinationEmails)
        Content messageSubject = new Content(subject)
        Body messageBody = new Body().withHtml(new Content(htmlBody))
        Message message = new Message(messageSubject, messageBody)
        try {
            SendEmailRequest sendEmailRequest = new SendEmailRequest(sourceEmail, destination, message)
            if (replyToEmail) {
                sendEmailRequest.replyToAddresses = [replyToEmail]
            }
            client.sendEmail(sendEmailRequest)
            status = EmailDeliveryStatus.STATUS_DELIVERED
        } catch (AmazonServiceException exception) {

            if (exception.message.find('Address blacklisted')) {
                log.debug "Address blacklisted destinationEmails=${destinationEmails}"
                status = EmailDeliveryStatus.STATUS_BLACKLISTED

            } else if (exception.message.find('Missing final')) {
                log.warn "An amazon service exception was catched while sending email: destinationEmails=$destinationEmails, sourceEmail=$sourceEmail, replyToEmail=$replyToEmail, subject=$subject"

            } else {
                log.warn 'An amazon service exception was catched while send +ng email' + exception.message
            }

        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was catched while sending email' + exception.message
        }
        status
    }

    /**
     *
     * @param destinationEmail
     * @param subject
     * @param htmlBody
     * @param sourceEmail
     * @param replyToEmail
     * @return status of delivery
     */
    @SuppressWarnings(['LineLength', 'ElseBlockBraces'])
    EmailDeliveryStatus send(
        String destinationEmail,
        String subject,
        String htmlBody,
        String sourceEmail = '',
        String replyToEmail = ''
    ) {
        send([destinationEmail], subject, htmlBody, sourceEmail, replyToEmail)
    }

    static TransactionalEmail transactionalEmailWithClosure(@DelegatesTo(value = TransactionalEmail, strategy = Closure.DELEGATE_FIRST) Closure composer) {
        Closure cl = composer.clone() as Closure
        TransactionalEmail transactionalEmail = new TransactionalEmail()
        cl.delegate = transactionalEmail
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        transactionalEmail
    }

}
