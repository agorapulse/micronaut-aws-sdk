package com.agorapulse.micronaut.aws.ses;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface SimpleEmailService {


    static TransactionalEmail email(Consumer<TransactionalEmail> composer) {
        TransactionalEmail email = new TransactionalEmail();
        composer.accept(email);

        if (email.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("Email does not contain recepients: " + email);
        }

        return email;
    }

    static TransactionalEmail email(@DelegatesTo(value = TransactionalEmail.class, strategy = Closure.DELEGATE_FIRST) Closure<TransactionalEmail> composer) {
        return email(ConsumerWithDelegate.create(composer));
    }

    default EmailDeliveryStatus send(Consumer<TransactionalEmail> composer) {
        return send(email(composer));
    }

    default EmailDeliveryStatus send(@DelegatesTo(value = TransactionalEmail.class, strategy = Closure.DELEGATE_FIRST) Closure<TransactionalEmail> composer) {
        return send(email(composer));
    }

    EmailDeliveryStatus send(TransactionalEmail email);
}
