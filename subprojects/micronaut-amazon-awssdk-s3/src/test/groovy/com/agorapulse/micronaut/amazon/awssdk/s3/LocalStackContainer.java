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
package com.agorapulse.micronaut.amazon.awssdk.s3;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.rnorth.ducttape.Preconditions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Container for Atlassian Labs Localstack, 'A fully functional local AWS cloud stack'.</p>
 * <p>{@link LocalStackContainer#withServices(Service...)} should be used to select which services
 * are to be launched. See {@link Service} for available choices. It is advised that
 * {@link LocalStackContainer#getEndpointOverride(Service)} and
 * {@link LocalStackContainer#getDefaultCredentialsProvider()}
 * be used to obtain compatible endpoint configuration and credentials, respectively.</p>
 */
public class LocalStackContainer extends GenericContainer<LocalStackContainer> {

    public static final String VERSION = "0.9.4";
    private static final String HOSTNAME_EXTERNAL_ENV_VAR = "HOSTNAME_EXTERNAL";

    private final List<Service> services = new ArrayList<>();

    public LocalStackContainer() {
        this(VERSION);
    }

    public LocalStackContainer(String version) {
        super(TestcontainersConfiguration.getInstance().getLocalStackImage() + ":" + version);

        withFileSystemBind("//var/run/docker.sock", "/var/run/docker.sock");
        waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));
    }

    @Override
    protected void configure() {
        super.configure();

        Preconditions.check("services list must not be empty", !services.isEmpty());

        withEnv("SERVICES", services.stream().map(Service::getLocalStackName).collect(Collectors.joining(",")));

        String hostnameExternalReason;
        if (getEnvMap().containsKey(HOSTNAME_EXTERNAL_ENV_VAR)) {
            // do nothing
            hostnameExternalReason = "explicitly as environment variable";
        } else if (getNetwork() != null && getNetworkAliases() != null && getNetworkAliases().size() >= 1) {
            withEnv(HOSTNAME_EXTERNAL_ENV_VAR, getNetworkAliases().get(getNetworkAliases().size() - 1));  // use the last network alias set
            hostnameExternalReason = "to match last network alias on container with non-default network";
        } else {
            withEnv(HOSTNAME_EXTERNAL_ENV_VAR, getContainerIpAddress());
            hostnameExternalReason = "to match host-routable address for container";
        }
        logger().info("{} environment variable set to {} ({})", HOSTNAME_EXTERNAL_ENV_VAR, getEnvMap().get(HOSTNAME_EXTERNAL_ENV_VAR), hostnameExternalReason);

        for (Service service : services) {
            addExposedPort(service.getPort());
        }
    }

    /**
     * Declare a set of simulated AWS services that should be launched by this container.
     * @param services one or more service names
     * @return this container object
     */
    public LocalStackContainer withServices(Service... services) {
        this.services.addAll(Arrays.asList(services));
        return self();
    }

    /**
     * TODO: update docs
     * Provides an endpoint configuration that is preconfigured to communicate with a given simulated service.
     * The provided endpoint configuration should be set in the AWS Java SDK when building a client, e.g.:
     * <pre><code>AmazonS3 s3 = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();
     </code></pre>
     *
     * <p><strong>Please note that this method is only intended to be used for configuring AWS SDK clients
     * that are running on the test host. If other containers need to call this one, they should be configured
     * specifically to do so using a Docker network and appropriate addressing.</strong></p>
     *
     * @param service the service that is to be accessed
     * @return an {@link URI}
     */
    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(Service service) {
        return new AwsClientBuilder.EndpointConfiguration(getEndpointOverride(service).toString(), getDefaultRegion());
    }

    public URI getEndpointOverride(Service service) {
        try {
            final String address = getContainerIpAddress();
            String ipAddress = address;
            // resolve IP address and use that as the endpoint so that path-style access is automatically used for S3
            ipAddress = InetAddress.getByName(address).getHostAddress();
            return new URI("http://"
                + ipAddress
                + ":"
                + getMappedPort(service.getPort()));
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    /**
     * Provides a {@link AWSCredentialsProvider} that is preconfigured to communicate with a given simulated service.
     * The credentials provider should be set in the AWS Java SDK when building a client, e.g.:
     * <pre><code>AmazonS3 s3 = AmazonS3ClientBuilder
     .standard()
     .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
     .withCredentials(localstack.getDefaultCredentialsProvider())
     .build();
     </code></pre>
     * @return an {@link AWSCredentialsProvider}
     */
    public AWSCredentialsProvider getDefaultCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(getDefaultAccessKey(), getDefaultSecretKey()));
    }

    public String getDefaultAccessKey() {
        return "accesskey";
    }

    public String getDefaultSecretKey() {
        return "secretkey";
    }

    public String getDefaultRegion() {
        return "us-east-1";
    }

    public enum Service {
        API_GATEWAY("apigateway", 4567),
        KINESIS("kinesis", 4568),
        DYNAMODB("dynamodb", 4569),
        DYNAMODB_STREAMS("dynamodbstreams", 4570),
        // TODO: Clarify usage for ELASTICSEARCH and ELASTICSEARCH_SERVICE
//        ELASTICSEARCH("es",           4571),
        S3("s3", 4572),
        FIREHOSE("firehose", 4573),
        LAMBDA("lambda", 4574),
        SNS("sns", 4575),
        SQS("sqs", 4576),
        REDSHIFT("redshift", 4577),
        //        ELASTICSEARCH_SERVICE("",   4578),
        SES("ses", 4579),
        ROUTE53("route53", 4580),
        CLOUDFORMATION("cloudformation", 4581),
        CLOUDWATCH("cloudwatch", 4582),
        SSM("ssm", 4583),
        SECRETSMANAGER("secretsmanager", 4584),
        STEPFUNCTIONS("stepfunctions", 4585),
        CLOUDWATCHLOGS("cloudwatchlogs", 4586),
        STS("sts", 4592),
        IAM("iam", 4593);

        final String localStackName;
        final int port;

        public String getLocalStackName() {
            return localStackName;
        }

        public int getPort() {
            return port;
        }

        Service(String localStackName, int port) {
            this.localStackName = localStackName;
            this.port = port;
        }
    }
}
