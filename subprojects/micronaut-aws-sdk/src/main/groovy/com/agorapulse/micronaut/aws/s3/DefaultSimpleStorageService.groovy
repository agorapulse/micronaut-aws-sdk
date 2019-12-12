/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.s3

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.*
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.multipart.PartData
import io.reactivex.Flowable
import org.apache.commons.codec.digest.DigestUtils

/**
 * Default implementation of the simple storage service.
 */
@Slf4j
@CompileStatic
@SuppressWarnings(
    [
        'NoWildcardImports',
        'SpaceAroundMapEntryColon',
    ]
)
class DefaultSimpleStorageService implements SimpleStorageService {

    private static final String ATTACHMENT = 'attachment'
    private static final Map<String, String> CONTENT_FLASH = Collections.singletonMap('contentType', 'application/x-shockwave-flash')
    private static final Map<String, String> CONTENT_BINARY = [contentType: 'application/octet-stream', contentDisposition: ATTACHMENT]

    private static final Map HTTP_CONTENTS = [
        audio: [contentType: 'audio/mpeg'],
        csv  : [contentType: 'text/csv', contentDisposition: ATTACHMENT],
        excel: [contentType: 'application/vnd.ms-excel', contentDisposition: ATTACHMENT],
        flash: CONTENT_FLASH,
        pdf  : [contentType: 'application/pdf'],
        file : CONTENT_BINARY,
        video: [contentType: 'video/x-flv'],
    ].asImmutable()

    private final AmazonS3 client
    private final String defaultBucketName
    TransferManager transferManager

    DefaultSimpleStorageService(AmazonS3 client, String defaultBucketName) {
        this.client = client
        this.defaultBucketName = defaultBucketName
    }

    /**
     *
     * @param type
     * @param fileExtension
     * @param cannedAcl
     * @return
     */
    @SuppressWarnings('DuplicateStringLiteral')
    static ObjectMetadata buildMetadataFromType(String type,
                                                String fileExtension,
                                                CannedAccessControlList cannedAcl = null) {
        Map contentInfo
        if (HTTP_CONTENTS[type]) {
            contentInfo = HTTP_CONTENTS[type] as Map
        } else if (type in ['image', 'photo']) {
            // Return image/jpeg for images to fix Safari issue (download image instead of inline display)
            contentInfo = [contentType: "image/${fileExtension == 'jpg' ? 'jpeg' : fileExtension}"]
        } else if (fileExtension == 'swf') {
            contentInfo = CONTENT_FLASH
        } else {
            contentInfo = CONTENT_BINARY
        }

        ObjectMetadata metadata = new ObjectMetadata()
        metadata.contentType = contentInfo.contentType as String
        if (contentInfo.contentDisposition) {
            metadata.contentDisposition = contentInfo.contentDisposition as String
        }
        if (cannedAcl) {
            metadata.setHeader('x-amz-acl', cannedAcl.toString())
        }
        metadata
    }

    @SuppressWarnings([
        'DuplicateStringLiteral',
        'UnnecessarySubstring',
    ])
    static String getBucketFromUri(String aURI) {
        URI uri = new URI(aURI)
        String path = uri.path.startsWith('/') ? uri.path.substring(1, uri.path.length()) : uri.path
        if (uri.host) {
            // direct bucket URI not using any CNAME
            if (uri.host.endsWith('amazonaws.com')) {
                if (uri.host.startsWith('s3')) {
                    return path.substring(0, path.indexOf('/'))
                }
                return uri.host.substring(0, uri.host.indexOf('.s3.'))
            }
            return uri.host
        }
        // consider the bucket name is using CNAME of the same name as the bucket
        return path.substring(0, path.indexOf('/'))
    }

    @SuppressWarnings([
        'DuplicateStringLiteral',
        'UnnecessarySubstring',
    ])
    static String getKeyFromUri(String aURI) {
        URI uri = new URI(aURI)
        String path = uri.path.startsWith('/') ? uri.path.substring(1, uri.path.length()) : uri.path
        if (uri.host) {
            // direct bucket URI not using any CNAME
            if (uri.host.endsWith('amazonaws.com') && uri.host.startsWith('s3')) {
                return path.substring(path.indexOf('/') + 1, path.length())
            }
            return path
        }
        // consider the bucket name is using CNAME of the same name as the bucket
        return path.substring(path.indexOf('/') + 1, path.length())
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @param contentType
     * @return
     */
    @SuppressWarnings('CouldBeElvis')
    Upload transferFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl) {
        if (!transferManager) {
            // Create transfer manager (only create if required, since it may pool connections and threads)
            transferManager = new TransferManager(client)
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, file)
            .withCannedAcl(cannedAcl)
        transferManager.upload(putObjectRequest)
    }

    /**
     *
     * @param bucketName
     * @param region
     */
    @SuppressWarnings('BuilderMethodWithSideEffects')
    void createBucket(String bucketName) {
        client.createBucket(bucketName)
    }

    /**
     *
     * @param bucketName
     */
    void deleteBucket(String bucketName) {
        client.deleteBucket(bucketName)
    }

    /**
     *
     * @param bucketName
     * @param key
     * @return
     */
    boolean deleteFile(String bucketName, String key) {
        try {
            client.deleteObject(bucketName, key)
            return true
        } catch (AmazonClientException exception) {
            log.warn "An amazon exception was catched while deleting a file $bucketName/$key", exception
        }
        return false
    }

    /**
     *
     * @param key
     * @return
     */
    boolean deleteFile(String key) {
        assertDefaultBucketName()
        deleteFile(defaultBucketName, key)
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param prefix
     * @return
     */
    @SuppressWarnings('DuplicateStringLiteral')
    boolean deleteFiles(String bucketName, String prefix) {
        assert prefix.tokenize('/').size() >= 2, 'Multiple delete are only allowed in sub/sub directories'

        Flowable<Boolean> results = listObjects(bucketName, prefix).flatMap {
            Flowable.fromIterable(it.objectSummaries)
        } map {
            deleteFile(bucketName, it.key)
        }

        return results.onErrorReturn {
            log.warn "Exception deleting objects in $bucketName/$prefix", it
            return false
        }.blockingIterable().every()
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param key
     * @return
     */
    boolean exists(String bucketName, String key) {
        if (!key) {
            return false
        }
        try {
            ObjectListing objectListing = client.listObjects(bucketName, key)
            if (objectListing.objectSummaries) {
                return true
            }
        } catch (AmazonS3Exception exception) {
            log.warn 'An amazon S3 exception was caught while checking if file exists', exception
        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was caught while checking if file exists', exception
        }
        return false
    }

    /**
     *
     * @param bucketName
     * @param key
     * @param localPath
     * @return
     */
    File getFile(String bucketName,
                 String key,
                 File localFile) {
        client.getObject(new GetObjectRequest(bucketName, key), localFile)
        localFile
    }

    /**
     *
     * @return
     */
    List listBucketNames() {
        client.listBuckets()*.name
    }

    /**
     *
     * @param bucketName
     * @param prefix
     * @return
     */
    Flowable<ObjectListing> listObjects(String bucketName, String prefix) {
        FlowableObjectListingHelper.generate(client, bucketName, prefix)
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param key
     * @param expirationDate
     * @return
     */
    String generatePresignedUrl(String bucketName,
                                String key,
                                Date expirationDate) {
        client.generatePresignedUrl(bucketName, key, expirationDate).toString()
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param input
     * @param metadata
     * @return
     */
    String storeInputStream(String bucketName,
                            String path,
                            InputStream input,
                            ObjectMetadata metadata) {
        try {
            client.putObject(bucketName, path, input, metadata)
        } catch (AmazonS3Exception exception) {
            log.warn 'An amazon S3 exception was catched while storing input stream', exception
            return ''
        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was catched while storing input stream', exception
            return ''
        }

        client.getUrl(bucketName, path)
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    String storeFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, file)
                .withCannedAcl(cannedAcl)
            client.putObject(putObjectRequest)
        } catch (AmazonClientException exception) {
            log.warn "An amazon client exception was catched while storing file $bucketName/$path", exception
            return ''
        }

        client.getUrl(bucketName, path)
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param partData
     * @param metadata
     * @return
     */
    @SuppressWarnings([
        'CouldBeElvis',
        'ParameterReassignment',
    ])
    String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl, ObjectMetadata metadata) {
        if (!metadata) {
            metadata = new ObjectMetadata()
        }
        metadata.setHeader(Headers.S3_CANNED_ACL, cannedAcl.toString())
        metadata.contentLength = partData.bytes.size()
        byte[] resultByte = DigestUtils.md5(partData.inputStream)
        metadata.contentMD5 = resultByte.encodeBase64().toString()
        partData.contentType.ifPresent { metadata.contentType = it.name }
        storeInputStream(bucketName, path, partData.inputStream, metadata)
    }

    String getDefaultBucketName() {
        assertDefaultBucketName()
        return defaultBucketName
    }

    /**
     * Move S3 object to different location (key).
     *
     * Moving objects is useful in combination with
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-set-lifecycle-configuration-intro.html">
     *     S3 Lifecycle Configurations
     * </a> for prefixes.
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
    ) {
        try {
            CopyObjectRequest request = new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey)

            S3Object object = client.getObject(sourceBucketName, sourceKey)

            if (object.taggingCount) {
                GetObjectTaggingRequest taggingRequest = new GetObjectTaggingRequest(sourceBucketName, sourceKey)
                GetObjectTaggingResult taggingResult = client.getObjectTagging(taggingRequest)
                request.withNewObjectTagging(new ObjectTagging(taggingResult.tagSet))
            }

            request.withNewObjectMetadata(object.objectMetadata)

            AccessControlList acl = client.getObjectAcl(sourceBucketName, sourceKey)
            AccessControlList newAcls = new AccessControlList()
            newAcls.grantAllPermissions(acl.grantsAsList as Grant[])
            request.withAccessControlList(newAcls)

            client.copyObject(request)
            client.deleteObject(sourceBucketName, sourceKey)
            return client.getUrl(destinationBucketName, destinationKey)
        } catch (AmazonClientException e) {
            log.error("Exception moving object $sourceBucketName/$sourceKey to $destinationBucketName/$destinationKey", e)
        }
        return null
    }

    // PRIVATE
    private void assertDefaultBucketName() {
        assert defaultBucketName, 'Default bucket must be defined'
    }
}
