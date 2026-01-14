/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Upload;
import io.micronaut.http.multipart.PartData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Service to simplify interaction with Amazon S3.
 */
public interface SimpleStorageService {

    static String getBucketFromUri(String aURI) {
        return DefaultSimpleStorageService.getBucketFromUri(aURI);
    }

    static String getKeyFromUri(String aURI) {
        return DefaultSimpleStorageService.getKeyFromUri(aURI);
    }

    /**
     * @return default name of the bucket
     */
    String getDefaultBucketName();

    /**
     * Tranfers the local file to S3.
     * @param path S3 path
     * @param file local file
     * @param cannedAcl ACLs
     * @return upload object to monitor the progress
     */
    default Upload transferFile(String path, File file, CannedAccessControlList cannedAcl) {
        return transferFile(getDefaultBucketName(), path, file, cannedAcl);
    }

    /**
     * Tranfers the local file to S3.
     * @param path S3 path
     * @param file local file
     * @return upload object to monitor the progress
     */
    default Upload transferFile(String path, File file) {
        return transferFile(path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * Tranfers the local file to S3.
     * @param bucketName the name of the bucket
     * @param path S3 path
     * @param file local file
     * @return upload object to monitor the progress
     */
    default Upload transferFile(String bucketName, String path, File file) {
        return transferFile(bucketName, path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * Tranfers the local file to S3.
     * @param bucketName the name of the bucket
     * @param path S3 path
     * @param file local file
     * @param cannedAcl ACLs
     * @return upload object to monitor the progress
     */
    Upload transferFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl);

    /**
     * Creates new bucket.
     * @param bucketName the name of the bucket.
     */
    void createBucket(String bucketName);

    /**
     * Creates new bucket of the name specified as <code>aws.s3.bucket</code> property.
     */
    default void createBucket() {
        createBucket(getDefaultBucketName());
    }

    /**
     * Deletes existing bucket.
     * @param bucketName the name of the bucket
     */
    void deleteBucket(String bucketName);

    /**
     * Creates new bucket of the name specified as <code>aws.s3.bucket</code> property.
     */
    default void deleteBucket() {
        deleteBucket(getDefaultBucketName());
    }

    /**
     * Delete file specified by the object key.
     * @param bucketName the name of the bucket
     * @param key the key of the object
     * @return true if the object has been deleted
     */
    boolean deleteFile(String bucketName, String key);

    /**
     * Delete file specified by the object key.
     * @param key the key of the object
     * @return true if the object has been deleted
     */
    boolean deleteFile(String key);

    /**
     * Delete all files specified by the common prefix (aka folder).
     * @param bucketName the name of the bucket
     * @param prefix common prefix of the objects to be deleted
     * @return true if the objects has been deleted
     */
    boolean deleteFiles(String bucketName, String prefix);

    /**
     * Delete all files specified by the common prefix (aka folder).
     * @param prefix common prefix of the objects to be deleted
     * @return true if the objects has been deleted
     */
    default boolean deleteFiles(String prefix) {
        return deleteFiles(getDefaultBucketName(), prefix);
    }

    /**
     * Check whether the object exists.
     * @param bucketName the name of the bucket
     * @param key the file key
     * @return true if the file with given key exist
     */
    boolean exists(String bucketName, String key);

    /**
     * Check whether the object exists.
     * @param key the file key
     * @return true if the file with given key exist
     */
    default boolean exists(String key) {
        return exists(getDefaultBucketName(), key);
    }

    /**
     * Downloads the file locally.
     * @param bucketName the name of the bucket
     * @param key the key of the object
     * @param localFile the destination file
     * @return the destination file
     */
    File getFile(String bucketName, String key, File localFile);

    /**
     * Downloads the file locally.
     * @param bucketName the name of the bucket
     * @param key the key of the object
     * @param localPath the destination file's path
     * @return the destination file
     */
    default File getFile(String bucketName, String key, String localPath) {
        return getFile(bucketName, key, new File(localPath));
    }


    /**
     * Downloads the file locally.
     * @param key the key of the object
     * @param localPath the destination file's path
     * @return the destination file
     */
    default File getFile(String key, File localPath) {
        return getFile(getDefaultBucketName(), key, localPath);
    }

    /**
     * Downloads the file locally.
     * @param key the key of the object
     * @param localPath the destination file's path
     * @return the destination file
     */
    default File getFile(String key, String localPath) {
        return getFile(getDefaultBucketName(), key, localPath);
    }

    /**
     * @return the list of all available buckets
     */
    List<String> listBucketNames();

    /**
     * Returns the flowable of ObjectListing which each contains list of objects.
     *
     * Use {@link #listObjectSummaries()} to get flowable of object summaries themselves.
     *
     * @param bucketName the name of the bucket
     * @param prefix the common prefix of the object being fetched
     * @return the list of all objects available as flowable of ObjectListing which each contains list of objects.
     */
    Publisher<ObjectListing> listObjects(String bucketName, String prefix);

    /**
     * Returns the flowable of ObjectListing which each contains list of objects.
     *
     * Use {@link #listObjectSummaries()} to get flowable of object summaries themselves.
     *
     * @param prefix the common prefix of the object being fetched
     * @return the flowable of ObjectListing which each contains list of objects
     */
    default Publisher<ObjectListing> listObjects(String prefix) {
        return listObjects(getDefaultBucketName(), prefix);
    }

    /**
     * Returns the flowable of ObjectListing which each contains list of objects.
     *
     * Use {@link #listObjectSummaries()} to get flowable of object summaries themselves.
     *
     * @return the flowable of ObjectListing which each contains list of objects
     */
    default Publisher<ObjectListing> listObjects() {
        return listObjects("");
    }

    /**
     * Returns the flowable of object summaries.
     *
     * @param bucketName the name of the bucket
     * @param prefix the common prefix of the object being fetched
     * @return the flowable of object summaries
     */
    default Publisher<S3ObjectSummary> listObjectSummaries(String bucketName, String prefix) {
        return Flux.from(listObjects(bucketName, prefix)).flatMap(l -> Flux.fromIterable(l.getObjectSummaries()));
    }

    /**
     * Returns the flowable of object summaries.
     *
     * @param prefix the common prefix of the object being fetched
     * @return the flowable of object summaries
     */
    default Publisher<S3ObjectSummary> listObjectSummaries(String prefix) {
        return listObjectSummaries(getDefaultBucketName(), prefix);
    }

    /**
     * Returns the flowable of object summaries.
     *
     * @return the flowable of object summaries
     */
    default Publisher<S3ObjectSummary> listObjectSummaries() {
        return listObjectSummaries("");
    }

    /**
     * Generates the pre-signed URL for downloading an object by clients.
     *
     * @param bucketName the name of the bucket
     * @param key the key of the object
     * @param expirationDate the expiration date of the link
     * @return the URL to download the object without any credentials required
     */
    String generatePresignedUrl(String bucketName, String key, Date expirationDate);

    /**
     * Generates the pre-signed URL for downloading an object by clients.
     *
     * @param key the key of the object
     * @param expirationDate the expiration date of the link
     * @return the URL to download the object without any credentials required
     */
    default String generatePresignedUrl(String key, Date expirationDate) {
        return generatePresignedUrl(getDefaultBucketName(), key, expirationDate);
    }

    /**
     * Generates the pre-signed URL for uploading an object by clients.
     *
     * @param bucketName the name of the bucket
     * @param key the key of the object
     * @param expirationDate the expiration date of the link
     * @return the URL to upload the object without any credentials required
     */
    String generateUploadUrl(String bucketName, String key, Date expirationDate);

    /**
     * Generates the pre-signed URL for uploading an object by clients.
     *
     * @param key the key of the object
     * @param expirationDate the expiration date of the link
     * @return the URL to download the object without any credentials required
     */
    default String generateUploadUrl(String key, Date expirationDate) {
        return generateUploadUrl(getDefaultBucketName(), key, expirationDate);
    }

    /**
     * Uploads data from the input stream to desired path on S3.
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param input the input data as stream
     * @param metadata the object metadata
     * @return the URL to newly created object
     */
    String storeInputStream(String bucketName, String path, InputStream input, ObjectMetadata metadata);

    /**
     * Uploads data from the input stream to desired path on S3.
     * @param path the destination path (key) on S3
     * @param input the input data as stream
     * @param metadata the object metadata
     * @return the URL to newly created object
     */
    default String storeInputStream(String path, InputStream input, ObjectMetadata metadata) {
        return storeInputStream(getDefaultBucketName(), path, input, metadata);
    }

    /**
     * Uploads data from the file to desired path on S3.
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param file the input file
     * @param cannedAcl ACLs
     * @return the URL to newly created object
     */
    String storeFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl);

    /**
     * Uploads data from the file to desired path on S3.
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param file the input file
     * @return the URL to newly created object
     */
    default String storeFile(String bucketName, String path, File file) {
        return storeFile(bucketName, path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * Uploads data from the file to desired path on S3.
     * @param path the destination path (key) on S3
     * @param file the input file
     * @param cannedAcl ACLs
     * @return the URL to newly created object
     */
    default String storeFile(String path, File file, CannedAccessControlList cannedAcl) {
        return storeFile(getDefaultBucketName(), path, file, cannedAcl);
    }

    /**
     * Uploads data from the file to desired path on S3.
     * @param path the destination path (key) on S3
     * @param file the input file
     * @return the URL to newly created object
     */
    default String storeFile(String path, File file) {
        return storeFile(getDefaultBucketName(), path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param partData the input data
     * @param cannedAcl ACLs
     * @param metadata the object metadata
     * @return the URL to newly created object
     */
    String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl, ObjectMetadata metadata);

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param partData the input data
     * @param cannedAcl ACLs
     * @return the URL to newly created object
     */
    default String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl) {
        return storeMultipartFile(bucketName, path, partData, cannedAcl, null);
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param partData the input data
     * @return the URL to newly created object
     */
    default String storeMultipartFile(String bucketName, String path, PartData partData) {
        return storeMultipartFile(bucketName, path, partData, CannedAccessControlList.PublicRead);
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param path the destination path (key) on S3
     * @param cannedAcl ACLs
     * @return the URL to newly created object
     */
    default String storeMultipartFile(String path, PartData multipartFile, CannedAccessControlList cannedAcl) {
        return storeMultipartFile(path, multipartFile, cannedAcl, null);
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param path the destination path (key) on S3
     * @param multipartFile the input data
     * @return the URL to newly created object
     */
    default String storeMultipartFile(String path, PartData multipartFile) {
        return storeMultipartFile(path, multipartFile, CannedAccessControlList.PublicRead);
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param path the destination path (key) on S3
     * @param multipartFile the input data
     * @param cannedAcl ACLs
     * @param metadata the object metadata
     * @return the URL to newly created object
     */
    default String storeMultipartFile(String path, PartData multipartFile, CannedAccessControlList cannedAcl, ObjectMetadata metadata) {
        return storeMultipartFile(getDefaultBucketName(), path, multipartFile, cannedAcl, metadata);
    }

    /**
     * Move S3 object to different location (key).
     *
     * Moving objects is useful in combination with <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-set-lifecycle-configuration-intro.html">S3 Lifecycle Configurations</a> for prefixes.
     *
     * @param sourceBucketName      the name of the source bucket
     * @param sourceKey             the key of the source object
     * @param destinationBucketName the name of the destination bucket
     * @param destinationKey        the key of the destination object
     * @return the destination URL or <code>null</code> if the file wasn't moved
     */
    String moveObject(
        String sourceBucketName,
        String sourceKey,
        String destinationBucketName,
        String destinationKey
    );

}
