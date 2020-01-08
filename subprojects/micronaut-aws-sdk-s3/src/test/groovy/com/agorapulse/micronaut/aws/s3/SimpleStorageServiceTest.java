/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.micronaut.context.ApplicationContext;
import io.reactivex.Flowable;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

// tag::header[]
public class SimpleStorageServiceTest {
    // end::header[]

    private static final String KEY = "foo/bar.baz";
    private static final String MY_BUCKET = "testbucket";
    private static final String SAMPLE_CONTENT = "hello world!";
    private static final String TEXT_FILE_PATH = "bar/foo.txt";
    private static final Date TOMORROW = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

    // tag::setup[]
    @Rule
    public final LocalStackContainer localstack = new LocalStackContainer()            // <1>
        .withServices(S3);

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private ApplicationContext ctx;                                                     // <2>

    @Before
    public void setup() {
        AmazonS3 amazonS3 = AmazonS3Client                                              // <3>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        ctx = ApplicationContext
            .build(Collections.singletonMap("aws.s3.bucket", MY_BUCKET))
            .build();
        ctx.registerSingleton(AmazonS3.class, amazonS3);                                // <4>
        ctx.start();
    }

    @After
    public void cleanup() {
        if (ctx != null) {                                                              // <5>
            ctx.close();
        }
    }
    // end::setup[]

    @Test
    public void testJavaService() throws IOException {
        SimpleStorageService service = ctx.getBean(SimpleStorageService.class);

        // tag::create-bucket[]
        service.createBucket(MY_BUCKET);                                                // <1>

        assertTrue(service.listBucketNames().contains(MY_BUCKET));                      // <2>
        // end::create-bucket[]

        // tag::store-file[]
        File sampleContent = createFileWithSampleContent();

        service.storeFile(TEXT_FILE_PATH, sampleContent);                               // <1>

        assertTrue(service.exists(TEXT_FILE_PATH));                                     // <2>

        Flowable<S3ObjectSummary> summaries = service.listObjectSummaries("foo");       // <3>
        assertEquals(Long.valueOf(0L), summaries.count().blockingGet());
        // end::store-file[]

        // CHECKSTYLE:OFF
        // tag::store-input-stream[]
        service.storeInputStream(                                                       // <1>
            KEY,
            new ByteArrayInputStream(SAMPLE_CONTENT.getBytes()),
            buildMetadata()
        );

        Flowable<S3ObjectSummary> fooSummaries = service.listObjectSummaries("foo");    // <2>
        assertEquals(KEY, fooSummaries.blockingFirst().getKey());
        // end::store-input-stream[]
        // CHECKSTYLE:ON

        // tag::generate-url[]
        String url = service.generatePresignedUrl(KEY, TOMORROW);                       // <1>

        assertEquals(SAMPLE_CONTENT, download(url));                                    // <2>
        // end::generate-url[]

        // tag::download-file[]
        File dir = tmp.newFolder();
        File file = new File(dir, "bar.baz");                                           // <1>

        service.getFile(KEY, file);                                                     // <2>
        assertTrue(file.exists());

        assertEquals(SAMPLE_CONTENT, new String(Files.readAllBytes(Paths.get(file.toURI()))));
        // end::download-file[]

        // tag::delete-file[]
        service.deleteFile(TEXT_FILE_PATH);                                             // <1>
        assertFalse(service.exists(TEXT_FILE_PATH));                                    // <2>
        // end::delete-file[]

        service.deleteFile(KEY);
        assertFalse(service.exists(KEY));

        // tag::delete-bucket[]
        service.deleteBucket();                                                         // <1>
        assertFalse(service.listBucketNames().contains(MY_BUCKET));                     // <2>
        // end::delete-bucket[]
    }

    private File createFileWithSampleContent() throws IOException {
        File file = tmp.newFile("foo.txt");
        file.createNewFile();

        Files.write(Paths.get(file.toURI()), SAMPLE_CONTENT.getBytes());
        return file;
    }

    private static ObjectMetadata buildMetadata() {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(SAMPLE_CONTENT.length());
        objectMetadata.setContentType("text/plain");
        objectMetadata.setContentDisposition("bar.baz");
        return objectMetadata;
    }

    private static String download(String url) throws IOException {
        return ResourceGroovyMethods.getText(new URL(url));
    }

}
