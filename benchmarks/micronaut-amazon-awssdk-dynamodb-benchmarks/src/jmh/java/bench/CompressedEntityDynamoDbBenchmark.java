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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceProvider;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDbService;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

@State(Scope.Benchmark)
public class CompressedEntityDynamoDbBenchmark {

    private ApplicationContext ctx;
    private LocalStackContainer localstack;
    private DynamoDbService<EntityWithCompression> compressionDynamoDbService;
    private DynamoDbService<EntityWithNoCompression> noCompressionDynamoDbService;
    private String jsonLarge;
    private String jsonSmall;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        boolean useAws = "true".equals(System.getProperty("useAws", "false"));

        final AwsCredentialsProvider credentialsProvider = createAwsCredentialsProvider(useAws);

        Map<String, Object> map = createProperties(useAws);

        ctx = ApplicationContext.builder(map).packages("entities").build();
        ctx.registerSingleton(AwsCredentialsProvider.class, credentialsProvider);
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

    private AwsCredentialsProvider createAwsCredentialsProvider(boolean useAws) {
        String accessKey = "";
        String secretKey = "";

        if (!useAws) {
            localstack = new LocalStackContainer().withServices(DYNAMODB);
            localstack.start();

            accessKey = localstack.getAccessKey();
            secretKey = localstack.getSecretKey();

        } else {
            accessKey = System.getenv("AWS_ACCESS_KEY_ID");
            if (accessKey == null) {
                throw new IllegalStateException("Export AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables before running the test.");
            }
            secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
            if (secretKey == null) {
                throw new IllegalStateException("Export AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables before running the test.");
            }
        }

        final AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(credentials);
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
