package com.agorapulse.micronaut.aws.ses;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

interface SimpleEmailService {

    static TransactionalEmail email(@DelegatesTo(value = TransactionalEmail.class, strategy = Closure.DELEGATE_FIRST) Closure composer) {
        Closure cl = (Closure) composer.clone();
        TransactionalEmail email = new TransactionalEmail();
        cl.setDelegate(email);
        cl.setResolveStrategy(Closure.DELEGATE_FIRST);
        cl.call();

        if (email.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("Email does not contain recepients: " + email);
        }

        return email;
    }

    default EmailDeliveryStatus send(@DelegatesTo(value = TransactionalEmail.class, strategy = Closure.DELEGATE_FIRST) Closure composer) {
        return send(email(composer));
    }

    EmailDeliveryStatus send(TransactionalEmail email);
}
