package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import io.reactivex.Emitter;
import io.reactivex.Flowable;

import java.util.List;

public class DefaultCloudWatchService implements CloudWatchService {

    private static class ListMetricsState {
        String nextToken;
    }

    private final DefaultCloudWatchConfiguration configuration;
    private final AmazonCloudWatch client;

    public DefaultCloudWatchService(DefaultCloudWatchConfiguration configuration, AmazonCloudWatch client) {
        this.configuration = configuration;
        this.client = client;
    }

    @Override
    public Flowable<Metric> getMetrics() {
        return Flowable.generate(ListMetricsState::new, (ListMetricsState state, Emitter<List<Metric>> emitter) -> {
            try {
                ListMetricsRequest listMetricsRequest = new ListMetricsRequest()
                    .withNamespace(configuration.getNamespace())
                    .withNextToken(state.nextToken);

                ListMetricsResult result = client.listMetrics(listMetricsRequest);
                emitter.onNext(result.getMetrics());

                if (result.getNextToken() == null) {
                    emitter.onComplete();
                } else {
                    state.nextToken = result.getNextToken();
                }
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).flatMap(Flowable::fromIterable);
    }
}
