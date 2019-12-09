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
