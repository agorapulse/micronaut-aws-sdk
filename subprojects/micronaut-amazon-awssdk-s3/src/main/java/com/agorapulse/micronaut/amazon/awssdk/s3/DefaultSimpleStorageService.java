package com.agorapulse.micronaut.amazon.awssdk.s3;

import io.micronaut.http.multipart.PartData;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DefaultSimpleStorageService implements SimpleStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSimpleStorageService.class);
    private static final String ALL_USERS = "http://acs.amazonaws.com/groups/global/AllUsers";
    private static final String AUTHENTICATED_USERS = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
    private static final String READ = "READ";

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

        Flowable<Boolean> results = listObjectSummaries(bucketName, prefix)
            .map(o -> deleteFile(bucketName, o.key()))
            .onErrorReturn(e -> {
                LOGGER.warn(String.format("Exception deleting objects in %s/%s", bucketName, prefix), e);
                return false;
            });

        return results.filter(r -> !r).count().blockingGet() == 0;
    }

    @Override
    public boolean exists(String bucketName, String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        try {
            return listObjectSummaries(bucketName, key).count().blockingGet() == 1;
        } catch (AwsServiceException | SdkClientException e) {
            LOGGER.warn(String.format("Exception obtaining object existence %s/%s", bucketName, key), e);
            return false;
        }
    }

    @Override
    public GetObjectResponse getObject(String bucketName, String key) {
        return s3.getObject(b -> b.bucket(bucketName).key(key)).response();
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
    public Flowable<ListObjectsV2Response> listObjects(String bucketName, String prefix) {
        return Flowable.fromIterable(s3.listObjectsV2Paginator(b -> b.bucket(bucketName).prefix(prefix)));
    }

    @Override
    public String generatePresignedUrl(String bucketName, String key, Date expirationDate) {
        PresignedGetObjectRequest request = presigner.presignGetObject(b ->
            b.getObjectRequest(r -> r.bucket(bucketName).key(key)).signatureDuration(Duration.between(Instant.now(), expirationDate.toInstant()))
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
    public String storeFile(String bucketName, String path, File file, ObjectCannedACL cannedAcl) {
        try {
            s3.putObject(
                b -> b.bucket(bucketName).key(path).acl(cannedAcl),
                RequestBody.fromFile(file)
            );
            return s3.utilities().getUrl(b -> b.bucket(bucketName).key(path)).toExternalForm();
        } catch (AwsServiceException exception) {
            return "";
        }
    }

    @Override
    public String storeMultipartFile(String bucketName, String path, PartData partData, ObjectCannedACL cannedAcl, Consumer<PutObjectRequest.Builder> additionalConfig) throws IOException {
        byte[] bytes = partData.getBytes();
        return storeInputStream(bucketName, path, partData.getInputStream(), b -> {
            b.acl(cannedAcl).contentLength(Integer.valueOf(bytes.length).longValue());
            partData.getContentType().ifPresent(t -> b.contentType(t.getName()));
            b.contentMD5(Md5Utils.md5AsBase64(bytes));
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
                extractCannedAcl(acl.grants()).ifPresent(b::acl);
            });

            s3.deleteObject(b -> b.bucket(sourceBucketName).key(sourceKey));

            return s3.utilities().getUrl(b -> b.bucket(destinationBucketName).key(destinationKey)).toExternalForm();
        } catch (AwsServiceException e) {
            LOGGER.error(String.format("Exception moving object %s/%s to %s/%s", sourceBucketName, sourceKey, destinationBucketName, destinationKey), e);
            return null;
        }
    }

    private static Optional<ObjectCannedACL> extractCannedAcl(List<Grant> grants) {
        for (Grant grant : grants) {
            if (READ.equals(grant.permissionAsString())) {
                if (ALL_USERS.equals(grant.grantee().uri())) {
                    return Optional.of(ObjectCannedACL.PUBLIC_READ);
                }
                if (AUTHENTICATED_USERS.equals(grant.grantee().uri())) {
                    return Optional.of(ObjectCannedACL.AUTHENTICATED_READ);
                }
            }
        }
        return Optional.empty();
    }

}
