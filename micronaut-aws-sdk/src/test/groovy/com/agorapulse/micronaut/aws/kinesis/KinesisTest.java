package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.Pogo;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import io.micronaut.context.ApplicationContext;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.junit.*;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

public class KinesisTest {

    public ApplicationContext context;

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer("0.8.8")
        .withServices(DYNAMODB, KINESIS);

    @Before
    public void setup() {
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");

        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        IDynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);

        AmazonKinesis amazonKinesis = AmazonKinesisClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(KINESIS))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        Map<String, Object> properties = new HashMap<>();
        properties.put("aws.kinesis.application.name", "TestApp");
        properties.put("aws.kinesis.stream", "MyStream");
        properties.put("aws.kinesis.listener.stream", "MyStream");

        context = ApplicationContext.build(properties).build();
        context.registerSingleton(AmazonKinesis.class, amazonKinesis);
        context.registerSingleton(AmazonDynamoDB.class, amazonDynamoDB);
        context.registerSingleton(IDynamoDBMapper.class, mapper);
        context.start();
    }

    @After
    public void cleanup() {
        System.clearProperty("com.amazonaws.sdk.disableCbor");
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testJavaService() throws InterruptedException {
        KinesisService service = context.getBean(KinesisService.class);
        KinesisListenerTester tester = context.getBean(KinesisListenerTester.class);
        DefaultClient client = context.getBean(DefaultClient.class);

        service.createStream();
        service.waitForActive();

        Disposable subscription = publishEventsAsync(tester, client);
        waitForRecievedMessages(tester, 120, 1000);

        subscription.dispose();

        Assert.assertTrue(allTestEventsReceived(tester));
    }

    private void waitForRecievedMessages(KinesisListenerTester tester, int retries, int waitMillis) throws InterruptedException {
        for (int i = 0; i < retries; i++) {
            if (!allTestEventsReceived(tester)) {
                Thread.sleep(waitMillis);
            }
        }
    }

    private Disposable publishEventsAsync(KinesisListenerTester tester, DefaultClient client) {
        return Flowable
            .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .takeWhile(t ->
                !allTestEventsReceived(tester)
            )
            .subscribe(t -> {
                try {
                    client.putEvent(new MyEvent("foo"));
                    client.putRecordDataObject("1234567890", new Pogo("bar"));
                } catch (Exception e) {
                    if (e.getMessage().contains("Unable to execute HTTP request")) {
                        // already finished
                        return;
                    }
                    throw e;
                }
            });
    }

    private static boolean allTestEventsReceived(KinesisListenerTester tester) {
        return tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenStringRecord")) &&
            tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenString")) &&
            tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenRecord")) &&
            tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObject")) &&
            tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObjectRecord")) &&
            tester.getExecutions().stream().anyMatch("EXECUTED: listenPogoRecord(com.agorapulse.micronaut.aws.Pogo(bar))"::equals);
    }
}
