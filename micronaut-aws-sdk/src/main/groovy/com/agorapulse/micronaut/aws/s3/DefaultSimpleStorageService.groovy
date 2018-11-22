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

@Slf4j
@CompileStatic
class DefaultSimpleStorageService implements SimpleStorageService {

    private static final Map HTTP_CONTENTS = [
            audio: [contentType: 'audio/mpeg'],
            csv:   [contentType: 'text/csv', contentDisposition: 'attachment'],
            excel: [contentType: 'application/vnd.ms-excel', contentDisposition: 'attachment'],
            flash: [contentType: 'application/x-shockwave-flash'],
            pdf:   [contentType: 'application/pdf'],
            file:  [contentType: 'application/octet-stream', contentDisposition: 'attachment'],
            video: [contentType: 'video/x-flv']
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
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @param contentType
     * @return
     */
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
     * @param type
     * @param fileExtension
     * @param cannedAcl
     * @return
     */
    static ObjectMetadata buildMetadataFromType(String type,
                                         String fileExtension,
                                         CannedAccessControlList cannedAcl = null) {
        Map contentInfo
        if (HTTP_CONTENTS[type]) {
            contentInfo = HTTP_CONTENTS[type] as Map
        } else if (type in ['image', 'photo']) {
            contentInfo = [contentType: "image/${fileExtension == 'jpg' ? 'jpeg' : fileExtension}"] // Return image/jpeg for images to fix Safari issue (download image instead of inline display)
        } else if (fileExtension == 'swf') {
            contentInfo = [contentType: "application/x-shockwave-flash"]
        } else {
            contentInfo = [contentType: 'application/octet-stream', contentDisposition: 'attachment']
        }

        ObjectMetadata metadata = new ObjectMetadata()
        metadata.setContentType(contentInfo.contentType as String)
        if (contentInfo.contentDisposition) {
            metadata.setContentDisposition(contentInfo.contentDisposition as String)
        }
        if (cannedAcl) {
            metadata.setHeader('x-amz-acl', cannedAcl.toString())
        }
        metadata
    }

    /**
     *
     * @param bucketName
     * @param region
     */
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
    boolean deleteFiles(String bucketName, String prefix) {
        assert prefix.tokenize('/').size() >= 2, "Multiple delete are only allowed in sub/sub directories"

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
     * @param prefix
     * @return
     */
    boolean exists(String bucketName, String prefix) {
        if (!prefix) {
            return false
        }
        try {
            ObjectListing objectListing = client.listObjects(bucketName, prefix)
            if (objectListing.objectSummaries) {
                return true
            }
        } catch (AmazonS3Exception exception) {
            log.warn 'An amazon S3 exception was catched while checking if file exists', exception
        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was catched while checking if file exists', exception
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
        client.listBuckets().collect { it.name }
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
    String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl, ObjectMetadata metadata) {
        if (!metadata) {
            metadata = new ObjectMetadata()
        }
        metadata.setHeader(Headers.S3_CANNED_ACL, cannedAcl.toString())
        metadata.setContentLength(partData.bytes.size())
        byte[] resultByte = DigestUtils.md5(partData.inputStream)
        metadata.setContentMD5(resultByte.encodeBase64().toString())
        partData.contentType.ifPresent { metadata.setContentType(it.name) }
        storeInputStream(bucketName, path, partData.inputStream, metadata)
    }

    String getDefaultBucketName() {
        assertDefaultBucketName()
        return defaultBucketName
    }

    // PRIVATE
    private void assertDefaultBucketName() {
        assert defaultBucketName, "Default bucket must be defined"
    }
}
