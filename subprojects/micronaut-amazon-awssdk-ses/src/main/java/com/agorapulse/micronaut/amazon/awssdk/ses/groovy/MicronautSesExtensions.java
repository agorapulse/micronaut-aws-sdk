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
