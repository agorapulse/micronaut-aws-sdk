/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.MessageTag;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static javax.mail.Message.RecipientType.TO;

/**
 * Default implementation of simple email service.
 */
public class DefaultSimpleEmailService implements SimpleEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSimpleEmailService.class);

    private final SesClient client;
    private final SimpleEmailServiceConfiguration configuration;

    DefaultSimpleEmailService(SesClient client, SimpleEmailServiceConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    public EmailDeliveryStatus send(TransactionalEmail email) {
        if (!email.getAttachments().isEmpty()) {
            try {
                return sendEmailWithAttachment(email);
            } catch (IOException | MessagingException exception) {
                LOGGER.error(
                    String.format(
                        "An exception was caught while sending email: destinationEmails=%s, from=%s, replyTo=%s, subject=%s",
                        email.getRecipients(),
                        email.getFrom(),
                        email.getReplyTo(),
                        email.getSubject()
                    ),
                    exception
                );
                return EmailDeliveryStatus.STATUS_NOT_DELIVERED;
            }
        }

        return sendWithoutAttachments(email);
    }

    private static EmailDeliveryStatus handleSend(TransactionalEmail email, Runnable c) {
        try {
            c.run();
            return EmailDeliveryStatus.STATUS_DELIVERED;
        } catch (AwsServiceException exception) {
            if (exception.getMessage().contains("blacklisted")) {
                LOGGER.warn(String.format("Address blacklisted destinationEmails=%s)", email.getRecipients()));
                return EmailDeliveryStatus.STATUS_BLACKLISTED;
            }
            LOGGER.error(
                String.format(
                    "An amazon service exception was caught while sending email: destinationEmails=%s, from=%s, replyTo=%s, subject=%s",
                    email.getRecipients(),
                    email.getFrom(),
                    email.getReplyTo(),
                    email.getSubject()
                ),
                exception
            );
            return EmailDeliveryStatus.STATUS_NOT_DELIVERED;
        }
    }

    private EmailDeliveryStatus sendEmailWithAttachment(TransactionalEmail email) throws MessagingException, IOException {
        Session session = Session.getInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(configuration.getSubjectPrefix().isPresent()
            ? configuration.getSubjectPrefix().get() + " " + email.getSubject()
            : email.getSubject()
        );

        if (!StringUtils.isEmpty(email.getFrom())) {
            mimeMessage.setFrom(new InternetAddress(email.getFrom()));
        } else if (configuration.getSourceEmail().isPresent()) {
            mimeMessage.setFrom(new InternetAddress(configuration.getSourceEmail().get()));
        }

        if (!StringUtils.isEmpty(email.getReplyTo())) {
            mimeMessage.setReplyTo(new InternetAddress[] {new InternetAddress(email.getReplyTo())});
        }

        for (String r : email.getRecipients()) {
            mimeMessage.addRecipients(TO, r);
        }

        MimeMultipart mimeMultipart = new MimeMultipart();

        BodyPart p = new MimeBodyPart();
        p.setContent(email.getHtmlBody(), "text/html");
        if (configuration.getUseBase64EncodingForMultipartEmails().orElse(false)) {
            p.setHeader("Content-Transfer-Encoding", "base64");
        }

        mimeMultipart.addBodyPart(p);

        for (TransactionalEmailAttachment attachment : email.getAttachments()) {
            if (!MimeType.isMimeTypeSupported(attachment.getMimeType())) {
                throw new UnsupportedAttachmentTypeException("Attachment type not supported for " + attachment.getFilename());
            }

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setFileName(attachment.getFilename());
            mimeBodyPart.setDescription(attachment.getDescription(), StandardCharsets.UTF_8.name());
            DataSource ds = new ByteArrayDataSource(new FileInputStream(new File(attachment.getFilepath())), attachment.getMimeType());
            mimeBodyPart.setDataHandler(new DataHandler(ds));
            mimeMultipart.addBodyPart(mimeBodyPart);
        }

        mimeMessage.setContent(mimeMultipart);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);

        SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
            .rawMessage(b -> b.data(SdkBytes.fromByteArray(outputStream.toByteArray())))
            .destinations(email.getRecipients())
            .source(Optional.ofNullable(email.getFrom()).orElseGet(() -> configuration.getSourceEmail().orElse(null)))
            .tags(getCustomTags(email))
            .configurationSetName(email.getConfigurationSetName())
            .build();

        return handleSend(email, () -> client.sendRawEmail(rawEmailRequest));
    }

    private EmailDeliveryStatus sendWithoutAttachments(TransactionalEmail email) {
        SendEmailRequest.Builder builder = SendEmailRequest.builder()
            .destination(b -> b.toAddresses(email.getRecipients()))
            .message(b -> {
                b.subject(c -> c.data(
                        configuration.getSubjectPrefix().isPresent()
                        ? configuration.getSubjectPrefix().get() + " " + email.getSubject()
                        : email.getSubject()
                    )
                );
                b.body(body -> body.html(c -> c.data(email.getHtmlBody())));
            })
            .source(Optional.ofNullable(email.getFrom()).orElseGet(() -> configuration.getSourceEmail().orElse(null)));

        if (email.getReplyTo() != null && email.getReplyTo().length() > 0) {
            builder.replyToAddresses(email.getReplyTo());
        }
        builder.tags(getCustomTags(email));
        builder.configurationSetName(email.getConfigurationSetName());

        return handleSend(email, () -> client.sendEmail(builder.build()));
    }

    private List<MessageTag> getCustomTags(TransactionalEmail email) {
        if (email.getTags() == null || email.getTags().isEmpty()) {
            return Collections.emptyList();
        }
        return email.getTags().entrySet().stream()
            .map(entry -> MessageTag.builder()
                .name(entry.getKey())
                .value(entry.getValue()).build())
            .collect(Collectors.toList());
    }

}
