/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.s3;

import io.micronaut.http.multipart.PartData;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultSimpleStorageService implements SimpleStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSimpleStorageService.class);
    private final S3Client s3;
    private final S3Presigner presigner;
    private final String defaultBucketName;

    public DefaultSimpleStorageService(String bucket, S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        defaultBucketName = bucket;
        this.presigner = presigner;
    }

    @Override
    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    @Override
    public void createBucket(String bucketName) {
        s3.createBucket(b -> b.bucket(bucketName));
    }

    @Override
    public void deleteBucket(String bucketName) {
        s3.deleteBucket(b -> b.bucket(bucketName));
    }

    @Override
    public boolean deleteFile(String bucketName, String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucketName).key(key));
            return true;
        } catch (AwsServiceException | SdkClientException e) {
            LOGGER.warn(String.format("Exception deleting object %s/%s", bucketName, key), e);
            return false;
        }
    }

    @Override
    public boolean deleteFiles(String bucketName, String prefix) {
        if (prefix.split("/").length >= 2) {
            throw new IllegalArgumentException("Multiple delete are only allowed in sub/sub directories: " + prefix);
        }

        return Flux.from(listObjectSummaries(bucketName, prefix))
            .map(o -> deleteFile(bucketName, o.key()))
            .onErrorResume(throwable -> {
                LOGGER.warn(String.format("Exception deleting objects in %s/%s", bucketName, prefix), throwable);
                return Mono.just(false);
            })
            .filter(r -> !r)
            .count()
            .blockOptional()
            .map(Long::intValue)
            .orElse(0) == 0;
    }

    @Override
    public boolean exists(String bucketName, String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        try {
            return getObject(bucketName, key) != null;
        } catch (AwsServiceException | SdkClientException e) {
            LOGGER.warn(String.format("Exception obtaining object existence %s/%s", bucketName, key), e);
            return false;
        }
    }

    @Override
    public GetObjectResponse getObject(String bucketName, String key) {
        try (ResponseInputStream<GetObjectResponse> stream = s3.getObject(b -> b.bucket(bucketName).key(key))) {
            return stream.response();
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception closing stream", e);
        }
    }

    @Override
    public File getFile(String bucketName, String key, File localFile) {
        s3.getObject(b -> b.bucket(bucketName).key(key), localFile.toPath());
        return localFile;
    }

    @Override
    public List<String> listBucketNames() {
        return s3.listBuckets().buckets().stream().map(Bucket::name).collect(Collectors.toList());
    }

    @Override
    public Publisher<ListObjectsV2Response> listObjects(String bucketName, String prefix) {
        return Flux.fromIterable(s3.listObjectsV2Paginator(b -> b.bucket(bucketName).prefix(prefix)));
    }

    @Override
    public String generatePresignedUrl(String bucketName, String key, Date expirationDate) {
        PresignedGetObjectRequest request = presigner.presignGetObject(b ->
            b.getObjectRequest(r -> r.bucket(bucketName).key(key)).signatureDuration(Duration.between(Instant.now(), expirationDate.toInstant()))
        );
        return request.url().toExternalForm();
    }

    @Override
    public String generateUploadUrl(String bucketName, String key, Date expirationDate) {
        PresignedPutObjectRequest request = presigner.presignPutObject(b ->
            b.putObjectRequest(r -> r.bucket(bucketName).key(key)).signatureDuration(Duration.between(Instant.now(), expirationDate.toInstant()))
        );
        return request.url().toExternalForm();
    }

    @Override
    public String storeInputStream(String bucketName, String path, InputStream input, Consumer<PutObjectRequest.Builder> additionalConfig) {
        try {
            s3.putObject(
                b -> {
                    additionalConfig.accept(b);
                    b.bucket(bucketName).key(path);
                },
                RequestBody.fromBytes(IoUtils.toByteArray(input))
            );
            return s3.utilities().getUrl(b -> b.bucket(bucketName).key(path)).toExternalForm();
        } catch (AwsServiceException | IOException exception) {
            return "";
        }
    }

    @Override
    public String storeFile(String bucketName, String path, File file, Consumer<PutObjectRequest.Builder> additionalConfig) {
        try {
            s3.putObject(
                b -> {
                    b.bucket(bucketName).key(path);
                    additionalConfig.accept(b);
                },
                RequestBody.fromFile(file)
            );
            return s3.utilities().getUrl(b -> b.bucket(bucketName).key(path)).toExternalForm();
        } catch (AwsServiceException exception) {
            return "";
        }
    }

    @Override
    public String storeMultipartFile(String bucketName, String path, PartData partData, Consumer<PutObjectRequest.Builder> additionalConfig) throws IOException {
        byte[] bytes = partData.getBytes();
        return storeInputStream(bucketName, path, partData.getInputStream(), b -> {
            b.contentLength(Integer.valueOf(bytes.length).longValue());
            b.contentMD5(Md5Utils.md5AsBase64(bytes));
            partData.getContentType().ifPresent(t -> b.contentType(t.getName()));
            additionalConfig.accept(b);
        });
    }

    @Override
    public String moveObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) {
        try {
            CopyObjectRequest.Builder request = CopyObjectRequest.builder()
                .copySource(sourceBucketName + "/" + sourceKey)
                .destinationBucket(destinationBucketName)
                .destinationKey(destinationKey);

            s3.copyObject(request.build());

            GetObjectAclResponse acl = s3.getObjectAcl(b -> b.bucket(sourceBucketName).key(sourceKey));
            s3.putObjectAcl(b -> {
                b.bucket(destinationBucketName).key(destinationKey).accessControlPolicy(p -> p.owner(acl.owner()).grants(acl.grants()));
            });

            s3.deleteObject(b -> b.bucket(sourceBucketName).key(sourceKey));

            return s3.utilities().getUrl(b -> b.bucket(destinationBucketName).key(destinationKey)).toExternalForm();
        } catch (AwsServiceException e) {
            LOGGER.error(String.format("Exception moving object %s/%s to %s/%s", sourceBucketName, sourceKey, destinationBucketName, destinationKey), e);
            return null;
        }
    }

}
