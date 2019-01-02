package com.agorapulse.micronaut.aws.ses

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString

/**
 * Email builder.
 */
@ToString
@CompileStatic
class TransactionalEmail {

    String from = ''
    String subject = ''
    String htmlBody = '<html><body></body></html>'
    String replyTo = ''

    List<String> recipients = []
    List<TransactionalEmailAttachment> attachments = []

    // Constructed via SimpleEmailService
    @SuppressWarnings('UnnecessaryConstructor')
    @PackageScope TransactionalEmail() { }

    void attachment(@DelegatesTo(value = TransactionalEmailAttachment, strategy = Closure.DELEGATE_FIRST) Closure attachment) {
        Closure cl = (Closure) attachment.clone()
        TransactionalEmailAttachment att = new TransactionalEmailAttachment()
        cl.delegate = att
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        attachments << att
    }

    @SuppressWarnings('ConfusingMethodName')
    void to(List<String> recipients) {
        this.recipients = recipients
    }

    @SuppressWarnings('ConfusingMethodName')
    void to(String... recipients) {
        this.to Arrays.asList(recipients)
    }

    @SuppressWarnings('ConfusingMethodName')
    void from(String str) {
        this.from = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void subject(String str) {
        this.subject = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void htmlBody(String str) {
        this.htmlBody = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void replyTo(String str) {
        this.replyTo = str
    }
}
