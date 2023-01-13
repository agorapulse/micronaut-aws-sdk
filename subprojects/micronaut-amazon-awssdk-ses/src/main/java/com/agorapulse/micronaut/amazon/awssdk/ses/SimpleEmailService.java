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
package com.agorapulse.micronaut.amazon.awssdk.ses;

import java.util.function.Consumer;

/**
 * Service for sending emails via AWS Simple Email Service.
 */
public interface SimpleEmailService {

    /**
     * Builds a new email.
     *
     * @param composer email definition
     * @return new email instance
     */
    static TransactionalEmail email(Consumer<TransactionalEmail> composer) {
        TransactionalEmail email = new TransactionalEmail();
        composer.accept(email);

        if (email.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("Email does not contain recepients: " + email);
        }

        return email;
    }

    /**
     * Builds and sends an email.
     * @param composer email definition
     * @return delivery status
     */
    default EmailDeliveryStatus send(Consumer<TransactionalEmail> composer) {
        return send(email(composer));
    }

    /**
     * Sends an email.
     * @param email email to be sent
     * @return delivery status
     */
    EmailDeliveryStatus send(TransactionalEmail email);

}
