package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.model.Metric;
import io.reactivex.Flowable;

public interface CloudWatchService {

    Flowable<Metric> getMetrics();

}
