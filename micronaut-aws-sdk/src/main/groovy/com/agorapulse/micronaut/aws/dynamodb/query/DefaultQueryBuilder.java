package com.agorapulse.micronaut.aws.dynamodb.query;

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBMetadata;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DefaultQueryBuilder<T> implements QueryBuilder<T> {

    DefaultQueryBuilder(Class<T> type, DynamoDBQueryExpression<T> expression) {
        this.metadata = DynamoDBMetadata.create(type);
        this.expression = expression;
    }

    public DefaultQueryBuilder<T> sort(Sort sort) {
        if (sort == Sort.ASC) {
            expression.withScanIndexForward(true);
        } else if (sort == Sort.DESC) {
            expression.withScanIndexForward(false);
        }
        return this;
    }

    public DefaultQueryBuilder<T> inconsistent(Read read) {
        if (read.equals(Read.READ)) {
            expression.withConsistentRead(false);
        }

        return this;
    }

    public DefaultQueryBuilder<T> index(String name) {
        expression.withIndexName(name);
        return this;
    }

    public DefaultQueryBuilder<T> hash(Object hash) {
        this.hashKey = hash;
        return this;
    }

    public DefaultQueryBuilder<T> range(Consumer<RangeConditionCollector<T>> conditions) {
        rangeCollectorsConsumers.add(conditions);
        return this;
    }

    public DefaultQueryBuilder<T> range(
        @DelegatesTo(value = RangeConditionCollector.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return range(ConsumerWithDelegate.create(conditions));
    }

    public DefaultQueryBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions) {
        filterCollectorsConsumers.add(conditions);
        return this;
    }

    public DefaultQueryBuilder<T> filter(
        @DelegatesTo(value = RangeConditionCollector.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    public DefaultQueryBuilder<T> filter(ConditionalOperator or) {
        expression.withConditionalOperator(or);
        return this;
    }

    public DefaultQueryBuilder<T> page(int page) {
        expression.withLimit(page);
        return this;
    }

    public DefaultQueryBuilder<T> offset(Object exclusiveStartKeyValue) {
        this.exclusiveStartKey = exclusiveStartKeyValue;
        return this;
    }

    @Override
    public Flowable<T> execute(IDynamoDBMapper mapper) {
        DynamoDBMapperTableModel<T> model = mapper.getTableModel(metadata.getItemClass());

        if (hashKey != null) {
            expression.withHashKeyValues(model.createKey(hashKey, null));
        }


        applyConditions(model, rangeCollectorsConsumers, expression::withRangeKeyCondition);
        applyConditions(model, filterCollectorsConsumers, expression::withQueryFilterEntry);


        if (exclusiveStartKey != null) {
            T exclusiveKey = model.createKey(exclusiveStartKey, null);
            expression.withExclusiveStartKey(model.convertKey(exclusiveKey));
        }


        Class<T> type = metadata.getItemClass();
        return Flowable.generate(() -> mapper.queryPage(type, expression), new BiFunction<QueryResultPage<T>, Emitter<List<T>>, QueryResultPage<T>>() {
            @Override
            public QueryResultPage<T> apply(QueryResultPage<T> result, Emitter<List<T>> emitter) throws Exception {
                emitter.onNext(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.onComplete();
                    return null;
                }

                return mapper.queryPage(type, expression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }
        }).flatMap(Flowable::fromIterable);
    }

    private void applyConditions(DynamoDBMapperTableModel<T> model, List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers, BiConsumer<String, Condition> addFilterConsumer) {
        if (!filterCollectorsConsumers.isEmpty()) {
            RangeConditionCollector<T> filterCollector = new RangeConditionCollector<>(model);

            for (Consumer<RangeConditionCollector<T>> consumer : filterCollectorsConsumers) {
                consumer.accept(filterCollector);
            }


            filterCollector.getConditions().forEach(addFilterConsumer);
        }
    }

    private final DynamoDBMetadata<T> metadata;
    private final DynamoDBQueryExpression<T> expression;
    private Object hashKey;
    private Object exclusiveStartKey;
    private List<Consumer<RangeConditionCollector<T>>> rangeCollectorsConsumers = new ArrayList<>();
    private List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers = new ArrayList<>();
}
