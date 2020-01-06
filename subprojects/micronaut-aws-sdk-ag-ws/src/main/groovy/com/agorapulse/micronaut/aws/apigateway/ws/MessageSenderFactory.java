/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.RequestContext;

public interface MessageSenderFactory {

    /**
     * Constructs connection URL based on the information from {@link RequestContext}.
     * @param context current request context
     * @return connection URL based on current {@link RequestContext}
     */
    static String createConnectionUrl(RequestContext context) {
        return "https://" + context.getDomainName() + "/" + context.getStage() + "/@connections";
    }

    /**
     * Creates new {@link MessageSender} or returns cached instance for given URL.
     * @param connectionsUrl the connection url String
     * @return new {@link MessageSender} or returns cached instance for given URL
     */
    MessageSender create(String connectionsUrl);

    /**
     * Creates new {@link MessageSender} or returns cached instance current request context.
     * @param context the current request context
     * @return new {@link MessageSender} or returns cached instance current request context
     */
    default MessageSender create(RequestContext context) {
        return create(createConnectionUrl(context));
    }

}
