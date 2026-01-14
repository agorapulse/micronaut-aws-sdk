/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Email builder.
 */
public class TransactionalEmail {

    private String from = "";
    private String subject = "";
    private String htmlBody = "<html><body></body></html>";
    private String replyTo = "";
    private String configurationSetName = "";
    private Map<String, String> tags = new HashMap<>();

    private final List<String> recipients = new ArrayList<>();
    private final List<TransactionalEmailAttachment> attachments = new ArrayList<>();

    // Constructed via SimpleEmailService
    TransactionalEmail() { }

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

    public TransactionalEmail configurationSetName(String str) {
        this.configurationSetName = str;
        return this;
    }

    public TransactionalEmail tags(Map<String, String> customTags) {
        this.tags = customTags;
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
        return Collections.unmodifiableList(recipients);
    }

    public List<TransactionalEmailAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public String getConfigurationSetName() {
        return configurationSetName;
    }

    public Map<String, String> getTags() {
        return tags;
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
            ", configurationSetName=" + configurationSetName +
            ", tags=" + tags +
            '}';
    }
    // CHECKSTYLE:ON
}
