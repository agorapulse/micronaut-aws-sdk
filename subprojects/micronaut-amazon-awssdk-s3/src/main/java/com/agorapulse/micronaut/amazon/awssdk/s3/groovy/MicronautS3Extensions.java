package com.agorapulse.micronaut.amazon.awssdk.s3.groovy;

import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageService;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import io.micronaut.http.multipart.PartData;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.io.IOException;
import java.io.InputStream;

public class MicronautS3Extensions {

    /**
     * Uploads data from the input stream to desired path on S3.
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param input the input data as stream
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeInputStream(
        SimpleStorageService self,
        String bucketName,
        String path,
        InputStream input,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) {
        return self.storeInputStream(bucketName, path, input, ConsumerWithDelegate.create(metadataDefinition));
    }

    /**
     * Uploads data from the input stream to desired path on S3.
     * @param path the destination path (key) on S3
     * @param input the input data as stream
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeInputStream(
        SimpleStorageService self,
        String path,
        InputStream input,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) {
        return storeInputStream(self, self.getDefaultBucketName(), path, input, metadataDefinition);
    }


    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param partData the input data
     * @param cannedAcl ACLs
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeMultipartFile(
        SimpleStorageService self,
        String bucketName,
        String path,
        PartData partData,
        ObjectCannedACL cannedAcl,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) throws IOException {
        return self.storeMultipartFile(bucketName, path, partData, cannedAcl, ConsumerWithDelegate.create(metadataDefinition));
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param path the destination path (key) on S3
     * @param multipartFile the input data
     * @param cannedAcl ACLs
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeMultipartFile(
        SimpleStorageService self,
        String path,
        PartData multipartFile,
        ObjectCannedACL cannedAcl,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) throws IOException {
        return storeMultipartFile(self, self.getDefaultBucketName(), path, multipartFile, cannedAcl, metadataDefinition);
    }

}
