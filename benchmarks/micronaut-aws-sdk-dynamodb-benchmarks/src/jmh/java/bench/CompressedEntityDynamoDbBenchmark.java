/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package bench;

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBService;
import com.agorapulse.micronaut.aws.dynamodb.DynamoDBServiceProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import entities.EntityWithCompression;
import entities.EntityWithNoCompression;
import io.micronaut.context.ApplicationContext;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

@State(Scope.Benchmark)
public class CompressedEntityDynamoDbBenchmark {

    private ApplicationContext ctx;
    private LocalStackContainer localstack;
    private AmazonDynamoDB amazonDynamoDB;
    private DynamoDBService<EntityWithCompression> compressionDynamoDbService;
    private DynamoDBService<EntityWithNoCompression> noCompressionDynamoDbService;
    private String jsonLarge;
    private String jsonSmall;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        boolean useAws = "true".equals(System.getProperty("useAws", "false"));

        amazonDynamoDB = createAmazonDynamoDB(useAws);

        IDynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);

        ctx = ApplicationContext.builder().packages("entities").build();
        ctx.registerSingleton(AmazonDynamoDB.class, amazonDynamoDB);
        ctx.registerSingleton(IDynamoDBMapper.class, mapper);
        ctx.start();

        compressionDynamoDbService = ctx.getBean(DynamoDBServiceProvider.class).findOrCreate(EntityWithCompression.class);
        try {
            compressionDynamoDbService.createTable();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        noCompressionDynamoDbService = ctx.getBean(DynamoDBServiceProvider.class).findOrCreate(EntityWithNoCompression.class);
        try {
            noCompressionDynamoDbService.createTable();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        jsonLarge = IOGroovyMethods.getText(this.getClass().getClassLoader().getResourceAsStream("large.json"));
        jsonSmall = IOGroovyMethods.getText(this.getClass().getClassLoader().getResourceAsStream("small.json"));
    }

    @Benchmark
    public void test_uncompressed_json_large() {
        noCompressionDynamoDbService.save(new EntityWithNoCompression(UUID.randomUUID().toString(), jsonLarge));
    }

    @Benchmark
    public void test_compressed_json_large() {
        compressionDynamoDbService.save(new EntityWithCompression(UUID.randomUUID().toString(), jsonLarge));
    }

    @Benchmark
    public void test_uncompressed_json_small() {
        noCompressionDynamoDbService.save(new EntityWithNoCompression(UUID.randomUUID().toString(), jsonSmall));
    }

    @Benchmark
    public void test_compressed_json_small() {
        compressionDynamoDbService.save(new EntityWithCompression(UUID.randomUUID().toString(), jsonSmall));
    }

    @TearDown(Level.Trial)
    public void cleanUp() {
        ctx.stop();

        if (localstack != null && localstack.isRunning()) {
            localstack.stop();
        }
    }

    private AmazonDynamoDB createAmazonDynamoDB(boolean useAws) {
        if (!useAws) {
            localstack = new LocalStackContainer().withServices(DYNAMODB);
            localstack.start();

            return AmazonDynamoDBClient.builder()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();
        }

        final String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        if (accessKey == null) {
            throwMissingAwsEnvVariableException();
        }

        final String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (secretKey == null) {
            throwMissingAwsEnvVariableException();
        }

        final String region = System.getenv("AWS_REGION");
        if (region == null) {
            throwMissingAwsEnvVariableException();
        }

        final AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(accessKey, secretKey)
        );

        return AmazonDynamoDBClient.builder()
            .withCredentials(awsStaticCredentialsProvider)
            .withRegion(region)
            .build();
    }

    private void throwMissingAwsEnvVariableException() {
        throw new IllegalStateException("Export AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_REGION environment variables before running the test.");
    }

    private Map<String, Object> createProperties(boolean useAws) {
        final Map<String, Object> properties = new HashMap<>();
        if (!useAws) {
            properties.put("aws.dynamodb.region", localstack.getRegion());
            properties.put("aws.dynamodb.endpoint", localstack.getEndpointConfiguration(DYNAMODB).getServiceEndpoint());
        } else {
            properties.put("aws.dynamodb.region", "eu-west-1");
        }
        return properties;
    }
}
