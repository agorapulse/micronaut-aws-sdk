package com.agorapulse.micronaut.aws.cloudwatch

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest
import com.amazonaws.services.cloudwatch.model.ListMetricsResult
import com.amazonaws.services.cloudwatch.model.Metric
import spock.lang.Specification

class CloudWatchServiceSpec extends Specification {

    public static final String NAMESPACE = "gr8conf"
    public static final String METRIC_NAME = 'hackergarten'
    public static final String NEXT_TOKEN = 'next-token'

    AmazonCloudWatch client = Mock()
    CloudWatchService service = new DefaultCloudWatchService(
        new DefaultCloudWatchConfiguration(namespace: NAMESPACE),
        client
    )

    void 'list metrics'() {
        when:
            List<Metric> metrics = service.getMetrics().toList().blockingGet()
        then:
            metrics
            metrics.size() == 2

            1 * client.listMetrics({ ListMetricsRequest r ->
                r.namespace == NAMESPACE && !r.nextToken
            }) >> new ListMetricsResult(metrics: [new Metric(
                namespace: NAMESPACE, metricName: METRIC_NAME

            )], nextToken: NEXT_TOKEN)

            1 * client.listMetrics({ ListMetricsRequest r ->
                r.namespace == NAMESPACE && r.nextToken
            }) >> new ListMetricsResult(metrics: [new Metric(
                namespace: NAMESPACE, metricName: METRIC_NAME.reverse()

            )])

    }

    void 'put metrics'() {
        when:
            service.putMetrics {
                name METRIC_NAME

                unit count

                labels 'DIM_NAME', 'DIM_VALUE'

                data 12, 2
                data 34, 1
                data 65, 4
            }
        then:
            noExceptionThrown()
    }

}
