package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class DefaultUpdateBuilder<T> implements UpdateBuilder<T> {

    private class Update {
        final String name;
        final AttributeAction action;
        final Object value;

        private Update(String name, AttributeAction action, Object value) {
            this.name = name;
            this.action = action;
            this.value = value;
        }
    }

    private final Class<T> itemType;
    private final List<Update> updates = new ArrayList<>();

    private Object hash;
    private Object range;

    private ReturnValue returnValue = ReturnValue.NONE;
    private Function<T, ?> returnValueMapper = Function.identity();

    DefaultUpdateBuilder(Class<T> itemType) {
        this.itemType = itemType;
    }

    @Override
    public UpdateBuilder<T> hash(Object key) {
        this.hash = key;
        return this;
    }

    @Override
    public UpdateBuilder<T> range(Object range) {
        this.range = range;
        return this;
    }

    @Override
    public UpdateBuilder<T> add(String attributeName, Object delta) {
        updates.add(new Update(attributeName, AttributeAction.ADD, delta));
        return this;
    }

    @Override
    public UpdateBuilder<T> put(String attributeName, Object value) {
        updates.add(new Update(attributeName, AttributeAction.PUT, value));
        return this;
    }

    @Override
    public UpdateBuilder<T> delete(String attributeName) {
        updates.add(new Update(attributeName, AttributeAction.DELETE, null));
        return this;
    }

    @Override
    public Object update(IDynamoDBMapper mapper, AmazonDynamoDB client) {
        UpdateItemRequest request = resolveExpression(mapper);
        UpdateItemResult result = client.updateItem(request);
        Map<String, AttributeValue> attributes = result.getAttributes();

        if (attributes.isEmpty()) {
            return null;
        }

        return returnValueMapper.apply(mapper.getTableModel(itemType).unconvert(attributes));
    }

    @Override
    public UpdateItemRequest resolveExpression(IDynamoDBMapper mapper) {
        DynamoDBMapperTableModel<T> tableModel = mapper.getTableModel(itemType);
        DynamoDBMetadata<T> metadata = DynamoDBMetadata.create(itemType);

        UpdateItemRequest request = new UpdateItemRequest()
            .withTableName(metadata.getMainTable().tableName())
            .withKey(tableModel.convertKey(hash, range))
            .withReturnValues(returnValue);

        for (Update u : updates) {
            request.addAttributeUpdatesEntry(
                u.name,
                new AttributeValueUpdate().withAction(u.action).withValue(tableModel.field(u.name).convert(u.value))
            );
        }

        return request;
    }

    @Override
    public UpdateBuilder<T> returns(ReturnValue returnValue, Function<T, ?> mapper) {
        this.returnValue = returnValue;
        this.returnValueMapper = mapper;

        return this;
    }
}
