package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

class FlowableObjectListingHelper {

    static Flowable<ObjectListing> generate(AmazonS3 client, String bucketName, String prefix) {
        return Flowable.generate(() -> client.listObjects(bucketName, prefix), new BiFunction<ObjectListing, Emitter<ObjectListing>, ObjectListing>() {
            @Override
            public ObjectListing apply(ObjectListing objectListing, Emitter<ObjectListing> emitter) throws Exception {
                emitter.onNext(objectListing);

                if (!objectListing.isTruncated()) {
                    emitter.onComplete();
                }

                return client.listNextBatchOfObjects(objectListing);
            }
        });
    }
}
