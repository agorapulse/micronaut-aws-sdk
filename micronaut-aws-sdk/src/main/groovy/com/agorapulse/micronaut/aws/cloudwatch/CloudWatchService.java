package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import io.reactivex.Flowable;

import java.util.function.Consumer;

public interface CloudWatchService {

    Flowable<Metric> getMetrics();

    void putMetrics(Consumer<MetricDatum> builder);

}
