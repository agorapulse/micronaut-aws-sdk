package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

import java.util.List;

class FlowableQueryResultHelper {

    public static <T> Flowable<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBQueryExpression<T> queryExpression) {
        return Flowable.generate(() -> mapper.queryPage(type, queryExpression), new BiFunction<QueryResultPage<T>, Emitter<List<T>>, QueryResultPage<T>>() {
            @Override
            public QueryResultPage<T> apply(QueryResultPage<T> result, Emitter<List<T>> emitter) throws Exception {
                emitter.onNext(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.onComplete();
                    return null;
                }

                return mapper.queryPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }
        }).flatMap(Flowable::fromIterable);
    }

    public static <T> Flowable<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBScanExpression queryExpression) {
        return Flowable.generate(() -> mapper.scanPage(type, queryExpression), new BiFunction<ScanResultPage<T>, Emitter<List<T>>, ScanResultPage<T>>() {
            @Override
            public ScanResultPage<T> apply(ScanResultPage<T> result, Emitter<List<T>> emitter) throws Exception {
                emitter.onNext(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.onComplete();
                    return null;
                }

                return mapper.scanPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }
        }).flatMap(Flowable::fromIterable);
    }

}
