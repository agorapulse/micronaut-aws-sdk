package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

import java.util.List;

public class FlowableListTopicHelper {


    static Flowable<Topic> generateTopics(AmazonSNS client) {
        return Flowable.generate(client::listTopics, new BiFunction<ListTopicsResult, Emitter<List<Topic>>, ListTopicsResult>() {
            @Override
            public ListTopicsResult apply(ListTopicsResult listTopicsResult, Emitter<List<Topic>> topicEmitter) {
                topicEmitter.onNext(listTopicsResult.getTopics());

                if (listTopicsResult.getNextToken() != null) {
                    return client.listTopics(listTopicsResult.getNextToken());
                }

                topicEmitter.onComplete();

                return null;
            }
        }).flatMap(Flowable::fromIterable);
    }

}
