package com.agorapulse.micronaut.amazon.awssdk.ses.groovy;

import com.agorapulse.micronaut.amazon.awssdk.ses.SimpleEmailService;
import com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

public class MicronautSesStaticExtensions {

    public static TransactionalEmail email(
            SimpleEmailService self,
            @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail", strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.ses.TransactionalEmail")
            Closure<TransactionalEmail> composer
    ) {
        return SimpleEmailService.email(ConsumerWithDelegate.create(composer));
    }

}
