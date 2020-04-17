package com.agorapulse.micronaut.amazon.awssdk.ses.groovy;

import com.agorapulse.micronaut.amazon.awssdk.ses.EmailDeliveryStatus;
import com.agorapulse.micronaut.amazon.awssdk.ses.SimpleEmailService;
import com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail;
import com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmailAttachment;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

public class MicronautSesExtensions {

    public static EmailDeliveryStatus send(
        SimpleEmailService self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail")
            Closure<TransactionalEmail> composer
    ) {
        return self.send(SimpleEmailService.email(ConsumerWithDelegate.create(composer)));
    }

    public static boolean asBoolean(EmailDeliveryStatus status) {
        return EmailDeliveryStatus.STATUS_DELIVERED.equals(status);
    }

    public static TransactionalEmail attachment(
        TransactionalEmail self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmailAttachment", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmailAttachment")
            Closure<TransactionalEmailAttachment> attachment
    ) {
        return self.attachment(ConsumerWithDelegate.create(attachment));
    }

}
