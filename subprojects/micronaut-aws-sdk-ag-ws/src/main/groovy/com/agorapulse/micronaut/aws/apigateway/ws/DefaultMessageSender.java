/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Default implementation of {@link MessageSender}.
 */
public class DefaultMessageSender implements MessageSender {

    private final String connectionsUrl;
    private final AWSCredentialsProvider credentialsProvider;
    private final String region;
    private final ObjectMapper mapper;

    public DefaultMessageSender(String connectionsUrl, AWSCredentialsProvider credentialsProvider, String region, ObjectMapper mapper) {
        this.connectionsUrl = connectionsUrl;
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.mapper = mapper;
    }

    @Override
    public void send(String connectionId, Object payload) {
        try {
            send(connectionId, mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize object to JSON", e);
        }
    }

    @Override
    public void send(String connectionId, InputStream payload) {
        String url = connectionsUrl + "/" + connectionId;

        Request<Void> request = new DefaultRequest<>("execute-api");
        request.setHttpMethod(HttpMethodName.POST);
        request.setEndpoint(URI.create(url));
        request.setContent(payload);

        AWS4Signer signer = new AWS4Signer();
        signer.setRegionName(region);
        signer.setServiceName(request.getServiceName());
        signer.sign(request, credentialsProvider.getCredentials());

        new AmazonHttpClient(new ClientConfiguration())
            .requestExecutionBuilder()
            .executionContext(new ExecutionContext(true))
            .request(request)
            .errorResponseHandler(new HttpResponseHandler<AmazonClientException>() {
                @Override
                public AmazonClientException handle(HttpResponse response) throws Exception {
                    if (response.getStatusCode() == 410) {
                        return new WebSocketClientGoneException(connectionId, "WebSocket client with id " + connectionId + " is already gone");
                    }
                    return new AmazonClientException("Exception publishing messages to WS endpoint "
                        + " POST " + url + " "
                        + response.getStatusCode() + ": "
                        + response.getStatusText() + "\n"
                        + readErrorMessage(response.getContent()));
                }

                @Override
                public boolean needsConnectionLeftOpen() {
                    return false;
                }
            }).execute(new HttpResponseHandler<Void>() {
            @Override
            public Void handle(HttpResponse response) {
                return null;
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return false;
            }
        });
    }

    private String readErrorMessage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
