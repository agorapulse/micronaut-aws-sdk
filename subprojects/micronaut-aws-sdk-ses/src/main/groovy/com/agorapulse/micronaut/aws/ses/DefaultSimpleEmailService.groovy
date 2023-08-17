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
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import java.nio.ByteBuffer

import static java.util.Collections.singletonList
import static javax.mail.Message.RecipientType.*

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

        return transactionalEmail
    }

    private final AmazonSimpleEmailService client
    private final SimpleEmailServiceConfiguration configuration

    DefaultSimpleEmailService(AmazonSimpleEmailService client, SimpleEmailServiceConfiguration configuration) {
        this.client = client
        this.configuration = configuration
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

    @SuppressWarnings(['LineLength', 'ElseBlockBraces', 'JavaIoPackageAccess', 'AbcMetric'])
    private EmailDeliveryStatus sendEmailWithAttachment(TransactionalEmail email) throws UnsupportedAttachmentTypeException {
        Session session = Session.getInstance(new Properties())
        MimeMessage mimeMessage = new MimeMessage(session)

        if (email.from) {
            mimeMessage.from = new InternetAddress(email.from)
        } else if (configuration.sourceEmail.present) {
            mimeMessage.from = new InternetAddress(configuration.sourceEmail.get())
        }

        if (email.replyTo) {
            mimeMessage.replyTo = [new InternetAddress(email.replyTo)] as InternetAddress[]
        }

        email.recipients.each { recipient ->
            mimeMessage.addRecipients(TO, new InternetAddress(recipient))
        }

        mimeMessage.subject = configuration.subjectPrefix.map { prefix ->
            "$prefix $email.subject".toString()
        } orElse(email.subject)

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
        rawEmailRequest.source = email.from ?: configuration.sourceEmail.orElse(null)
        rawEmailRequest.tags = getTags(email)

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

        String subject = configuration.subjectPrefix.map { prefix ->
            "$prefix $email.subject".toString()
        } orElse(email.subject)

        Content messageSubject = new Content(subject)

        Body messageBody = new Body().withHtml(new Content(email.htmlBody))
        Message message = new Message(messageSubject, messageBody)

        return handleSend(email) {
            String from = email.from ?: configuration.sourceEmail.orElse(null)
            SendEmailRequest sendEmailRequest = new SendEmailRequest(from, destination, message)
            if (email.replyTo) {
                sendEmailRequest.replyToAddresses = singletonList(email.replyTo)
            }
            sendEmailRequest.tags = getTags(email)
            client.sendEmail(sendEmailRequest)
        }
    }

    private List<MessageTag> getTags(TransactionalEmail email) {
        if (!email.tags) {
            return []
        }
        return email.tags.collect { entry ->
            new MessageTag()
                .withName(entry.key)
                .withValue(entry.value)
        }
    }

}
