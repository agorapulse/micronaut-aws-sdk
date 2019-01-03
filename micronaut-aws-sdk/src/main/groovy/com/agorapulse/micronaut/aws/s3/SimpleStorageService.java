package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Upload;
import io.micronaut.http.multipart.PartData;
import io.reactivex.Flowable;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface SimpleStorageService {


    String getDefaultBucketName();

    /**
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    default Upload transferFile(String path, File file, CannedAccessControlList cannedAcl) {
        return transferFile(getDefaultBucketName(), path, file, cannedAcl);
    }

    /**
     * @param path
     * @param file
     * @return
     */
    default Upload transferFile(String path, File file) {
        return transferFile(path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * @param bucketName
     * @param path
     * @param file
     * @return
     */
    default Upload transferFile(String bucketName, String path, File file) {
        return transferFile(bucketName, path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    Upload transferFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl);

    /**
     * @param bucketName
     */
    void createBucket(String bucketName);

    /**
     * Creates new bucket of the name specified as <code>aws.s3.bucket</code> property.
     */
    default void createBucket() {
        createBucket(getDefaultBucketName());
    }

    /**
     * @param bucketName
     */
    void deleteBucket(String bucketName);

    /**
     * Creates new bucket of the name specified as <code>aws.s3.bucket</code> property.
     */
    default void deleteBucket() {
        deleteBucket(getDefaultBucketName());
    }

    /**
     * @param bucketName
     * @param key
     * @return
     */
    boolean deleteFile(String bucketName, String key);

    /**
     * @param key
     * @return
     */
    boolean deleteFile(String key);

    /**
     * @param bucketName
     * @param prefix
     * @return
     */
    boolean deleteFiles(String bucketName, String prefix);

    /**
     * @param prefix
     * @return
     */
    default boolean deleteFiles(String prefix) {
        return deleteFiles(getDefaultBucketName(), prefix);
    }

    /**
     * @param bucketName
     * @param prefix
     * @return
     */
    boolean exists(String bucketName, String prefix);

    /**
     * @param prefix
     * @return
     */
    default boolean exists(String prefix) {
        return exists(getDefaultBucketName(), prefix);
    }

    /**
     * @param bucketName
     * @param key
     * @param localFile
     * @return
     */
    File getFile(String bucketName, String key, File localFile);

    /**
     * @param bucketName
     * @param key
     * @param localPath
     * @return
     */
    default File getFile(String bucketName, String key, String localPath) {
        return getFile(bucketName, key, new File(localPath));
    }


    /**
     * @param key
     * @param localPath
     * @return
     */
    default File getFile(String key, File localPath) {
        return getFile(getDefaultBucketName(), key, localPath);
    }

    /**
     * @param key
     * @param localPath
     * @return
     */
    default File getFile(String key, String localPath) {
        return getFile(getDefaultBucketName(), key, localPath);
    }

    /**
     * @return
     */
    List<String> listBucketNames();

    /**
     * @param bucketName
     * @param prefix
     * @return
     */
    Flowable<ObjectListing> listObjects(String bucketName, String prefix);

    /**
     * @param prefix
     * @return
     */
    default Flowable<ObjectListing> listObjects(String prefix) {
        return listObjects(getDefaultBucketName(), prefix);
    }

    /**
     * @return
     */
    default Flowable<ObjectListing> listObjects() {
        return listObjects("");
    }

    /**
     * @param bucketName
     * @param prefix
     * @return
     */
    default Flowable<S3ObjectSummary> listObjectSummaries(String bucketName, String prefix) {
        return listObjects(bucketName, prefix).flatMap(l -> Flowable.fromIterable(l.getObjectSummaries()));
    }

    /**
     * @param prefix
     * @return
     */
    default Flowable<S3ObjectSummary> listObjectSummaries(String prefix) {
        return listObjectSummaries(getDefaultBucketName(), prefix);
    }

    /**
     * @return
     */
    default Flowable<S3ObjectSummary> listObjectSummaries() {
        return listObjectSummaries("");
    }

    /**
     * @param bucketName
     * @param key
     * @param expirationDate
     * @return
     */
    String generatePresignedUrl(String bucketName, String key, Date expirationDate);

    /**
     * @param key
     * @param expirationDate
     * @return
     */
    default String generatePresignedUrl(String key, Date expirationDate) {
        return generatePresignedUrl(getDefaultBucketName(), key, expirationDate);
    }

    /**
     * @param bucketName
     * @param path
     * @param input
     * @param metadata
     * @return
     */
    String storeInputStream(String bucketName, String path, InputStream input, ObjectMetadata metadata);

    /**
     * @param path
     * @param input
     * @param metadata
     * @return
     */
    default String storeInputStream(String path, InputStream input, ObjectMetadata metadata) {
        return storeInputStream(getDefaultBucketName(), path, input, metadata);
    }

    /**
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    String storeFile(String bucketName, String path, File file, CannedAccessControlList cannedAcl);

    /**
     * @param bucketName
     * @param path
     * @param file
     * @return
     */
    default String storeFile(String bucketName, String path, File file) {
        return storeFile(bucketName, path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    default String storeFile(String path, File file, CannedAccessControlList cannedAcl) {
        return storeFile(getDefaultBucketName(), path, file, cannedAcl);
    }

    /**
     * @param path
     * @param file
     * @return
     */
    default String storeFile(String path, File file) {
        return storeFile(getDefaultBucketName(), path, file, CannedAccessControlList.PublicRead);
    }

    /**
     * @param bucketName
     * @param path
     * @param partData
     * @param metadata
     * @return
     */
    String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl, ObjectMetadata metadata);

    /**
     * @param bucketName
     * @param path
     * @param partData
     * @return
     */
    default String storeMultipartFile(String bucketName, String path, PartData partData, CannedAccessControlList cannedAcl) {
        return storeMultipartFile(bucketName, path, partData, cannedAcl, null);
    }

    /**
     * @param bucketName
     * @param path
     * @param partData
     * @return
     */
    default String storeMultipartFile(String bucketName, String path, PartData partData) {
        return storeMultipartFile(bucketName, path, partData, CannedAccessControlList.PublicRead);
    }

    /**
     * @param path
     * @param multipartFile
     * @param cannedAcl
     * @return
     */
    default String storeMultipartFile(String path, PartData multipartFile, CannedAccessControlList cannedAcl) {
        return storeMultipartFile(path, multipartFile, cannedAcl, null);
    }

    /**
     * @param path
     * @param multipartFile
     * @return
     */
    default String storeMultipartFile(String path, PartData multipartFile) {
        return storeMultipartFile(path, multipartFile, CannedAccessControlList.PublicRead);
    }

    /**
     * @param path
     * @param multipartFile
     * @param cannedAcl
     * @param metadata
     * @return
     */
    default String storeMultipartFile(String path, PartData multipartFile, CannedAccessControlList cannedAcl, ObjectMetadata metadata) {
        return storeMultipartFile(getDefaultBucketName(), path, multipartFile, cannedAcl, metadata);
    }

}
