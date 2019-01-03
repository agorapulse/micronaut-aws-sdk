package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.kinesis.worker.WorkerStateListener;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.cloudwatch.waiters.AmazonCloudWatchWaiters;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
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

// tag::testcontainers-header[]
public class KinesisTest {
// end::testcontainers-header[]

    // tag::testcontainers-setup[]
    public ApplicationContext context;                                                  // <1>

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer("0.8.8")            // <2>
        .withServices(DYNAMODB, KINESIS);

    @Before
    public void setup() {
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");                    // <3>

        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClient                            // <4>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        AmazonKinesis amazonKinesis = AmazonKinesisClient                               // <5>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(KINESIS))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        AmazonCloudWatch cloudWatch = new MockCloudWatch();

        Map<String, Object> properties = new HashMap<>();                               // <6>
        properties.put("aws.kinesis.application.name", "TestApp");
        properties.put("aws.kinesis.stream", "MyStream");
        properties.put("aws.kinesis.listener.stream", "MyStream");

        context = ApplicationContext.build(properties).build();                         // <7>
        context.registerSingleton(AmazonKinesis.class, amazonKinesis);
        context.registerSingleton(AmazonDynamoDB.class, amazonDynamoDB);
        context.registerSingleton(AmazonCloudWatch.class, cloudWatch);
        context.registerSingleton(AWSCredentialsProvider.class, localstack.getDefaultCredentialsProvider());
        context.start();
    }

    @After
    public void cleanup() {
        System.clearProperty("com.amazonaws.sdk.disableCbor");                          // <8>
        if (context != null) {
            context.close();                                                            // <9>
        }
    }
    // end::testcontainers-setup[]

    // tag::testcontainers-test[]
    @Test
    public void testJavaService() throws InterruptedException {
        KinesisService service = context.getBean(KinesisService.class);                 // <10>
        KinesisListenerTester tester = context.getBean(KinesisListenerTester.class);    // <11>
        DefaultClient client = context.getBean(DefaultClient.class);                    // <12>

        service.createStream();
        service.waitForActive();

        waitForWorkerReady(120, 100);
        Disposable subscription = publishEventsAsync(tester, client);
        waitForRecievedMessages(tester, 120, 100);

        subscription.dispose();

        Assert.assertTrue(allTestEventsReceived(tester));
    }
    // end::testcontainers-test[]

    private void waitForRecievedMessages(KinesisListenerTester tester, int retries, int waitMillis) throws InterruptedException {
        for (int i = 0; i < retries; i++) {
            if (!allTestEventsReceived(tester)) {
                Thread.sleep(waitMillis);
            }
        }
    }

    private void waitForWorkerReady(int retries, int waitMillis) throws InterruptedException {
        WorkerStateListener listener = context.getBean(WorkerStateListener.class);
        for (int i = 0; i < retries; i++) {
            if (!listener.isReady("MyStream")) {
                Thread.sleep(waitMillis);
            }
        }
        if (!listener.isReady("MyStream")) {
            throw new IllegalStateException("Worker not ready yet after " + retries * waitMillis + " milliseconds");
        }
        System.err.println("Worker is ready");
        Thread.sleep(waitMillis);
    }

    private Disposable publishEventsAsync(KinesisListenerTester tester, DefaultClient client) {
        return Flowable
            .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .takeWhile(t ->
                !allTestEventsReceived(tester)
            )
            .subscribe(t -> {
                try {
                    System.err.println("Publishing events");
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
        return tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenStringRecord"))
            && tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenString"))
            && tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenRecord"))
            && tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObject"))
            && tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObjectRecord"))
            && tester.getExecutions().stream().anyMatch("EXECUTED: listenPogoRecord(com.agorapulse.micronaut.aws.Pogo(bar))"::equals);
    }

    private static class MockCloudWatch implements AmazonCloudWatch {
        @Override
        public void setEndpoint(String s) {

        }

        @Override
        public void setRegion(Region region) {

        }

        @Override
        public DeleteAlarmsResult deleteAlarms(DeleteAlarmsRequest deleteAlarmsRequest) {
            return null;
        }

        @Override
        public DeleteDashboardsResult deleteDashboards(DeleteDashboardsRequest deleteDashboardsRequest) {
            return null;
        }

        @Override
        public DescribeAlarmHistoryResult describeAlarmHistory(DescribeAlarmHistoryRequest describeAlarmHistoryRequest) {
            return null;
        }

        @Override
        public DescribeAlarmHistoryResult describeAlarmHistory() {
            return null;
        }

        @Override
        public DescribeAlarmsResult describeAlarms(DescribeAlarmsRequest describeAlarmsRequest) {
            return null;
        }

        @Override
        public DescribeAlarmsResult describeAlarms() {
            return null;
        }

        @Override
        public DescribeAlarmsForMetricResult describeAlarmsForMetric(DescribeAlarmsForMetricRequest describeAlarmsForMetricRequest) {
            return null;
        }

        @Override
        public DisableAlarmActionsResult disableAlarmActions(DisableAlarmActionsRequest disableAlarmActionsRequest) {
            return null;
        }

        @Override
        public EnableAlarmActionsResult enableAlarmActions(EnableAlarmActionsRequest enableAlarmActionsRequest) {
            return null;
        }

        @Override
        public GetDashboardResult getDashboard(GetDashboardRequest getDashboardRequest) {
            return null;
        }

        @Override
        public GetMetricDataResult getMetricData(GetMetricDataRequest getMetricDataRequest) {
            return null;
        }

        @Override
        public GetMetricStatisticsResult getMetricStatistics(GetMetricStatisticsRequest getMetricStatisticsRequest) {
            return null;
        }

        @Override
        public GetMetricWidgetImageResult getMetricWidgetImage(GetMetricWidgetImageRequest getMetricWidgetImageRequest) {
            return null;
        }

        @Override
        public ListDashboardsResult listDashboards(ListDashboardsRequest listDashboardsRequest) {
            return null;
        }

        @Override
        public ListMetricsResult listMetrics(ListMetricsRequest listMetricsRequest) {
            return null;
        }

        @Override
        public ListMetricsResult listMetrics() {
            return null;
        }

        @Override
        public PutDashboardResult putDashboard(PutDashboardRequest putDashboardRequest) {
            return null;
        }

        @Override
        public PutMetricAlarmResult putMetricAlarm(PutMetricAlarmRequest putMetricAlarmRequest) {
            return null;
        }

        @Override
        public PutMetricDataResult putMetricData(PutMetricDataRequest putMetricDataRequest) {
            return null;
        }

        @Override
        public SetAlarmStateResult setAlarmState(SetAlarmStateRequest setAlarmStateRequest) {
            return null;
        }

        @Override
        public void shutdown() {

        }

        @Override
        public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
            return null;
        }

        @Override
        public AmazonCloudWatchWaiters waiters() {
            return null;
        }
    }
}
