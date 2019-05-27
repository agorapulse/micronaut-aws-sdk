package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.model.MetricDatum;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MetricData {

    private final List<MetricDatum> data = new ArrayList<>();

    MetricData metric(Consumer<MetricDatum> datum) {
        MetricDatum d = new MetricDatum();
        datum.accept(d);
        data.add(d);
        return this;
    }

    public List<MetricDatum> getData() {
        return data;
    }
}
