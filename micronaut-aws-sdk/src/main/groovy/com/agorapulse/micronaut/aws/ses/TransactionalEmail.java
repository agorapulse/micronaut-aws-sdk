package com.agorapulse.micronaut.aws.ses;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Email builder.
 */
public class TransactionalEmail {

    private String from = "";
    private String subject = "";
    private String htmlBody = "<html><body></body></html>";
    private String replyTo = "";

    private final List<String> recipients = new ArrayList<>();
    private final List<TransactionalEmailAttachment> attachments = new ArrayList<>();

    // Constructed via SimpleEmailService
    TransactionalEmail() { }

    public TransactionalEmail attachment(@DelegatesTo(value = TransactionalEmailAttachment.class, strategy = Closure.DELEGATE_FIRST) Closure attachment) {
        return attachment(ConsumerWithDelegate.create(attachment));
    }

    public TransactionalEmail attachment(Consumer<TransactionalEmailAttachment> attachment) {
        TransactionalEmailAttachment a = new TransactionalEmailAttachment();
        attachment.accept(a);
        attachments.add(a);
        return this;
    }

    public TransactionalEmail to(List<String> recipients) {
        this.recipients.addAll(recipients);
        return this;
    }

    public TransactionalEmail to(String... recipients) {
        this.to(Arrays.asList(recipients));
        return this;
    }

    public TransactionalEmail from(String str) {
        this.from = str;
        return this;
    }

    public TransactionalEmail subject(String str) {
        this.subject = str;
        return this;
    }

    public TransactionalEmail htmlBody(String str) {
        this.htmlBody = str;
        return this;
    }

    public TransactionalEmail replyTo(String str) {
        this.replyTo = str;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public List<TransactionalEmailAttachment> getAttachments() {
        return attachments;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "TransactionalEmail{" +
            "from='" + from + '\'' +
            ", subject='" + subject + '\'' +
            ", htmlBody='" + htmlBody + '\'' +
            ", replyTo='" + replyTo + '\'' +
            ", recipients=" + recipients +
            ", attachments=" + attachments +
            '}';
    }
    // CHECKSTYLE:ON
}
