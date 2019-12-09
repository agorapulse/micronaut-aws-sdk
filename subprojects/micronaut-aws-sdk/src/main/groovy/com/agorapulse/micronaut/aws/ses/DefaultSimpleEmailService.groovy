package com.agorapulse.micronaut.aws.ses

import com.amazonaws.AmazonClientException
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
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

import static java.util.Collections.singletonList

/**
 * Default implementation of simple email service.
 */
@Slf4j
@Singleton
@CompileStatic
@Requires(classes = AmazonSimpleEmailService)
@SuppressWarnings([
    'NoWildcardImports',
    'DuplicateStringLiteral',
])
class DefaultSimpleEmailService implements SimpleEmailService {

    static TransactionalEmail email(@DelegatesTo(value = TransactionalEmail, strategy = Closure.DELEGATE_FIRST) Closure composer) {
        Closure cl = composer.clone() as Closure
        TransactionalEmail transactionalEmail = new TransactionalEmail()
        cl.delegate = transactionalEmail
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if (!transactionalEmail.recipients) {
            throw new IllegalArgumentException("Email does not contain recepients: $transactionalEmail")
        }

        transactionalEmail
    }

    private final AmazonSimpleEmailService client

    DefaultSimpleEmailService(AmazonSimpleEmailService client) {
        this.client = client
    }

    EmailDeliveryStatus send(TransactionalEmail email) throws Exception {
        if (email.attachments) {
            return sendEmailWithAttachment(email)
        }

        return sendWithoutAttachments(email)
    }

    private static EmailDeliveryStatus handleSend(TransactionalEmail email, Closure c) {
        try {
            c()
            return EmailDeliveryStatus.STATUS_DELIVERED
        } catch (AmazonClientException exception) {
            if (exception.message.find('Address blacklisted')) {
                log.debug "Address blacklisted destinationEmails=${email.recipients}"
                return EmailDeliveryStatus.STATUS_BLACKLISTED
            }
            log.warn "An amazon service exception was catched while sending email: destinationEmails=$email.recipients," +
                " from=$email.from), replyTo=$email.replyTo, subject=$email.subject", exception
            return EmailDeliveryStatus.STATUS_NOT_DELIVERED
        }
    }

    @SuppressWarnings(['LineLength', 'ElseBlockBraces', 'JavaIoPackageAccess'])
    private EmailDeliveryStatus sendEmailWithAttachment(TransactionalEmail email) throws UnsupportedAttachmentTypeException {
        Session session = Session.getInstance(new Properties())
        MimeMessage mimeMessage = new MimeMessage(session)
        String subject = email.subject
        mimeMessage.subject = subject
        MimeMultipart mimeMultipart = new MimeMultipart()

        BodyPart p = new MimeBodyPart()
        p.setContent(email.htmlBody, 'text/html')
        mimeMultipart.addBodyPart(p)

        for (TransactionalEmailAttachment attachment : email.attachments) {
            if (!MimeType.isMimeTypeSupported(attachment.mimeType)) {
                throw new UnsupportedAttachmentTypeException()
            }

            MimeBodyPart mimeBodyPart = new MimeBodyPart()
            mimeBodyPart.fileName = attachment.filename
            mimeBodyPart.setDescription(attachment.description, 'UTF-8')
            DataSource ds = new ByteArrayDataSource(new FileInputStream(new File(attachment.filepath)), attachment.mimeType)
            mimeBodyPart.dataHandler = new DataHandler(ds)
            mimeMultipart.addBodyPart(mimeBodyPart)
        }

        mimeMessage.content = mimeMultipart

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()))

        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)

        rawEmailRequest.destinations = email.recipients
        rawEmailRequest.source = email.from

        return handleSend(email) {
            client.sendRawEmail(rawEmailRequest)
            return EmailDeliveryStatus.STATUS_DELIVERED
        }
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
    private EmailDeliveryStatus sendWithoutAttachments(TransactionalEmail email) {
        Destination destination = new Destination(email.recipients)
        Content messageSubject = new Content(email.subject)
        Body messageBody = new Body().withHtml(new Content(email.htmlBody))
        Message message = new Message(messageSubject, messageBody)

        return handleSend(email) {
            SendEmailRequest sendEmailRequest = new SendEmailRequest(email.from, destination, message)
            if (email.replyTo) {
                sendEmailRequest.replyToAddresses = singletonList(email.replyTo)
            }
            client.sendEmail(sendEmailRequest)
        }
    }
}
