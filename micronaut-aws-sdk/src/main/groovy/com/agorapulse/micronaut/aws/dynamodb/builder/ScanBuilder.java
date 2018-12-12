package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface ScanBuilder<T> extends DetachedScan<T> {

    ScanBuilder<T> consistent(Builders.Read read);
    ScanBuilder<T> inconsistent(Builders.Read read);

    ScanBuilder<T> index(String name);
    ScanBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    default ScanBuilder<T> filter(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    ScanBuilder<T> filter(ConditionalOperator or);

    ScanBuilder<T> page(int page);

    ScanBuilder<T> offset(Object exclusiveStartKeyValue);

}
