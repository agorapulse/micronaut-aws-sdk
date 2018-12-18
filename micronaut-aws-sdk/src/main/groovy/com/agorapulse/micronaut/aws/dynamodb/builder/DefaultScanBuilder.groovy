package com.agorapulse.micronaut.aws.dynamodb.builder

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBMetadata
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.reactivex.Flowable

import java.util.function.BiConsumer
import java.util.function.Consumer

@PackageScope
@CompileStatic
class DefaultScanBuilder<T> implements ScanBuilder<T> {

    DefaultScanBuilder(Class<T> type, DynamoDBScanExpression expression) {
        this.metadata = DynamoDBMetadata.create(type)
        this.expression = expression
    }

    DefaultScanBuilder<T> inconsistent(Builders.Read read) {
        if (read.equals(Builders.Read.READ)) {
            expression.withConsistentRead(false)
        }

        return this
    }

    DefaultScanBuilder<T> consistent(Builders.Read read) {
        if (read.equals(Builders.Read.READ)) {
            expression.withConsistentRead(true)
        }

        return this
    }

    DefaultScanBuilder<T> index(String name) {
        expression.withIndexName(name)
        return this
    }


    DefaultScanBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions) {
        filterCollectorsConsumers.add(conditions)
        return this
    }

    DefaultScanBuilder<T> filter(ConditionalOperator or) {
        expression.withConditionalOperator(or)
        return this
    }

    DefaultScanBuilder<T> page(int page) {
        expression.withLimit(page)
        return this
    }

    DefaultScanBuilder<T> offset(Object exclusiveHashStartKeyValue, Object exclusiveRangeStartKeyValue) {
        this.exclusiveHashStartKey = exclusiveHashStartKeyValue
        this.exclusiveRangeStartKey = exclusiveRangeStartKeyValue
        return this
    }

    @Override
    int count(IDynamoDBMapper mapper) {
        return mapper.count(metadata.getItemClass(), resolveExpression(mapper))
    }

    @Override
    Flowable<T> scan(IDynamoDBMapper mapper) {
        return FlowableQueryResultHelper.generate(metadata.itemClass, mapper, resolveExpression(mapper))
    }

    @Override
    DynamoDBScanExpression resolveExpression(IDynamoDBMapper mapper) {
        DynamoDBMapperTableModel<T> model = mapper.getTableModel(metadata.getItemClass())

        applyConditions(model, filterCollectorsConsumers, expression.&withFilterConditionEntry)

        if (exclusiveHashStartKey != null || exclusiveRangeStartKey != null) {
            T exclusiveKey = model.createKey(exclusiveHashStartKey, exclusiveRangeStartKey)
            expression.withExclusiveStartKey(model.convertKey(exclusiveKey))
        }

        configurer.accept(expression)

        return expression
    }

    @Override
    ScanBuilder<T> configure(Consumer<DynamoDBScanExpression> configurer) {
        this.configurer = configurer
        return this
    }

    // for proper groovy evaluation of closure in the annotation
    Object getProperty(String name) {
        throw new MissingPropertyException("No properties here!")
    }

    private void applyConditions(DynamoDBMapperTableModel<T> model, List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers, BiConsumer<String, Condition> addFilterConsumer) {
        if (!filterCollectorsConsumers.isEmpty()) {
            RangeConditionCollector<T> filterCollector = new RangeConditionCollector<>(model)

            for (Consumer<RangeConditionCollector<T>> consumer : filterCollectorsConsumers) {
                consumer.accept(filterCollector)
            }

            filterCollector.getConditions().forEach(addFilterConsumer)
        }
    }

    private final DynamoDBMetadata<T> metadata
    private final DynamoDBScanExpression expression
    private Object exclusiveHashStartKey
    private Object exclusiveRangeStartKey
    private List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers = new ArrayList<>()
    private Consumer<DynamoDBScanExpression> configurer = {} as Consumer<DynamoDBScanExpression>
}
