/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.s3.groovy;

import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageService;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import io.micronaut.http.multipart.PartData;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MicronautS3Extensions {

    /**
     * Uploads data from the file to desired path on S3.
     * @param path the destination path (key) on S3
     * @param file the input file
     * @return the URL to newly created object
     */
    public static String storeFile(
        SimpleStorageService self,
        String path,
        File file,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
            Closure<PutObjectRequest.Builder> metadataDefinition
    ) {
        return self.storeFile(path, file, ConsumerWithDelegate.create(metadataDefinition));
    }

    /**
     * Uploads data from the file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param file the input file
     * @return the URL to newly created object
     */
    public static String storeFile(
        SimpleStorageService self,
        String bucketName,
        String path,
        File file,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
            Closure<PutObjectRequest.Builder> metadataDefinition
    ) {
        return self.storeFile(bucketName, path, file, ConsumerWithDelegate.create(metadataDefinition));
    }

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
        return self.storeInputStream(path, input, ConsumerWithDelegate.create(metadataDefinition));
    }


    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param bucketName the name of the bucket
     * @param path the destination path (key) on S3
     * @param partData the input data
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeMultipartFile(
        SimpleStorageService self,
        String bucketName,
        String path,
        PartData partData,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) throws IOException {
        return self.storeMultipartFile(bucketName, path, partData, ConsumerWithDelegate.create(metadataDefinition));
    }

    /**
     * Uploads data from the multipart file to desired path on S3.
     *
     * @param path the destination path (key) on S3
     * @param multipartFile the input data
     * @param metadataDefinition the object metadata definition
     * @return the URL to newly created object
     */
    public static String storeMultipartFile(
        SimpleStorageService self,
        String path,
        PartData multipartFile,
        @DelegatesTo(type = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder")
        Closure<PutObjectRequest.Builder> metadataDefinition
    ) throws IOException {
        return self.storeMultipartFile(path, multipartFile, ConsumerWithDelegate.create(metadataDefinition));
    }

}
