package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CloudWatchExtensions {

    public static void putMetrics(
        CloudWatchService service,
        @DelegatesTo(value = MetricData.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.aws.cloudwatch.MetricData")
            Closure<MetricData> builder
    ) {
        service.putMetrics(ConsumerWithDelegate.create(builder));
    }

    public static MetricData metric(
        MetricData data,
        @DelegatesTo(value = MetricDatum.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.amazonaws.services.cloudwatch.model.MetricDatum")
            Closure<MetricDatum> builder
    ) {
        return data.metric(ConsumerWithDelegate.create(builder));
    }

    public static MetricDatum name(MetricDatum datum, String metricName) {
        return datum.withMetricName(metricName);
    }

    public static MetricDatum labels(MetricDatum datum, String name, String value) {
        return dimension(datum, name, value);
    }

    public static MetricDatum dimension(MetricDatum datum, String name, String value) {
        Dimension dimension = new Dimension().withName(name).withValue(value);
        if (datum.getDimensions() == null) {
            datum.withDimensions(dimension);
        } else {
            datum.getDimensions().add(dimension);
        }
        return datum;
    }

    public static MetricDatum timestamp(MetricDatum datum, Date timestamp) {
        return datum.withTimestamp(timestamp);
    }

    public static MetricDatum value(MetricDatum datum, Double value) {
        return datum.withValue(value);
    }

    public static MetricDatum statisticValues(
        MetricDatum datum,
        @DelegatesTo(value = StatisticSet.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.amazonaws.services.cloudwatch.model.StatisticSet")
            Closure<StatisticSet> statisticValues
    ) {
        StatisticSet stats = new StatisticSet();
        ConsumerWithDelegate.create(statisticValues).accept(stats);
        return datum.withStatisticValues(stats);
    }

    public static MetricDatum values(MetricDatum datum, List<Double> values) {
        if (datum.getValues() == null) {
            datum.withValues(values);
        } else {
            datum.getValues().addAll(values);
        }
        return datum;
    }

    public static MetricDatum values(MetricDatum datum, Double... values) {
        return values(datum, Arrays.asList(values));
    }

    public static MetricDatum data(MetricDatum datum, Double value) {
        return values(datum, value);
    }

    public static MetricDatum data(MetricDatum datum, Double value, Double count) {
        values(datum, value);
        counts(datum, count);
        return datum;
    }

    public static MetricDatum counts(MetricDatum datum, Double... counts) {
        return datum.withCounts(counts);
    }

    public static MetricDatum counts(MetricDatum datum, List<Double> counts) {
        return datum.withCounts(counts);
    }

    public static MetricDatum unit(MetricDatum datum, StandardUnit unit) {
        return datum.withUnit(unit);
    }

    public static MetricDatum storageResolution(MetricDatum datum, Integer storageResolution) {
        return datum.withStorageResolution(storageResolution);
    }

    public static StatisticSet sampleCount(StatisticSet stats, Double sampleCount) {
        return stats.withSampleCount(sampleCount);
    }

    public static StatisticSet sum(StatisticSet stats, Double sum) {
        return stats.withSum(sum);
    }

    public static StatisticSet minimum(StatisticSet stats, Double minimum) {
        return stats.withMinimum(minimum);
    }

    public static StatisticSet maximum(StatisticSet stats, Double maximum) {
        return stats.withMaximum(maximum);
    }

    public static StandardUnit getSeconds(MetricDatum datum) {
        return StandardUnit.Seconds;
    }

    public static StandardUnit getMicroseconds(MetricDatum datum) {
        return StandardUnit.Microseconds;
    }

    public static StandardUnit getMilliseconds(MetricDatum datum) {
        return StandardUnit.Milliseconds;
    }

    public static StandardUnit getBytes(MetricDatum datum) {
        return StandardUnit.Bytes;
    }

    public static StandardUnit getKilobytes(MetricDatum datum) {
        return StandardUnit.Kilobytes;
    }

    public static StandardUnit getMegabytes(MetricDatum datum) {
        return StandardUnit.Megabytes;
    }

    public static StandardUnit getGigabytes(MetricDatum datum) {
        return StandardUnit.Gigabytes;
    }

    public static StandardUnit getTerabytes(MetricDatum datum) {
        return StandardUnit.Terabytes;
    }

    public static StandardUnit getBits(MetricDatum datum) {
        return StandardUnit.Bits;
    }

    public static StandardUnit getKilobits(MetricDatum datum) {
        return StandardUnit.Kilobits;
    }

    public static StandardUnit getMegabits(MetricDatum datum) {
        return StandardUnit.Megabits;
    }

    public static StandardUnit getGigabits(MetricDatum datum) {
        return StandardUnit.Gigabits;
    }

    public static StandardUnit getTerabits(MetricDatum datum) {
        return StandardUnit.Terabits;
    }

    public static StandardUnit getPercent(MetricDatum datum) {
        return StandardUnit.Percent;
    }

    public static StandardUnit getCount(MetricDatum datum) {
        return StandardUnit.Count;
    }

    public static StandardUnit getBytesSecond(MetricDatum datum) {
        return StandardUnit.BytesSecond;
    }

    public static StandardUnit getKilobytesSecond(MetricDatum datum) {
        return StandardUnit.KilobytesSecond;
    }

    public static StandardUnit getMegabytesSecond(MetricDatum datum) {
        return StandardUnit.MegabytesSecond;
    }

    public static StandardUnit getGigabytesSecond(MetricDatum datum) {
        return StandardUnit.GigabytesSecond;
    }

    public static StandardUnit getTerabytesSecond(MetricDatum datum) {
        return StandardUnit.TerabytesSecond;
    }

    public static StandardUnit getBitsSecond(MetricDatum datum) {
        return StandardUnit.BitsSecond;
    }

    public static StandardUnit getKilobitsSecond(MetricDatum datum) {
        return StandardUnit.KilobitsSecond;
    }

    public static StandardUnit getMegabitsSecond(MetricDatum datum) {
        return StandardUnit.MegabitsSecond;
    }

    public static StandardUnit getGigabitsSecond(MetricDatum datum) {
        return StandardUnit.GigabitsSecond;
    }

    public static StandardUnit getTerabitsSecond(MetricDatum datum) {
        return StandardUnit.TerabitsSecond;
    }

    public static StandardUnit getCountSecond(MetricDatum datum) {
        return StandardUnit.CountSecond;
    }

    public static StandardUnit getNone(MetricDatum datum) {
        return StandardUnit.None;
    }


}
