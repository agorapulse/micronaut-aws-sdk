/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import com.amazonaws.services.s3.model.ObjectMetadata
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.TempDir

import javax.inject.Inject

/**
 * Tests for SimpleStorageService based on Testcontainers.
 */
@SuppressWarnings('NoJavaUtilDate')
@Stepwise
// tag::header[]
@MicronautTest                                                                          // <1>
@Property(name = 'aws.s3.bucket', value = MY_BUCKET)                                    // <2>
class SimpleStorageServiceSpec extends Specification {

// end::header[]

    private static final String KEY = 'foo/bar.baz'
    private static final String UPLOAD_KEY = 'foo/foo.two'
    private static final String MY_BUCKET = 'testbucket'
    private static final String SAMPLE_CONTENT = 'hello world!'
    private static final Date TOMORROW = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)

    @TempDir File tmp

    // tag::setup[]
    @Inject SimpleStorageService service                                                // <3>
    // end::setup[]

    void 'new bucket'() {
        when:
            service.createBucket(MY_BUCKET)
        then:
            service.listBucketNames().contains(MY_BUCKET)
    }

    void 'upload file'() {
        when:
            File file = new File(tmp, 'foo.txt')
            file.createNewFile()
            file.text = SAMPLE_CONTENT

            service.storeFile('bar/foo.txt', file)
        then:
            service.exists('bar/foo.txt')
    }

    void 'upload content'() {
        expect:
            Flux.from(service.listObjectSummaries('foo')).count().block() == 0
        when:
            service.storeInputStream(
                KEY,
                new ByteArrayInputStream(SAMPLE_CONTENT.bytes),
                new ObjectMetadata(
                    contentLength: SAMPLE_CONTENT.size(),
                    contentType: 'text/plain',
                    contentDisposition: 'bar.baz'
                )
            )
        then:
            Flux.from(service.listObjectSummaries('foo')).blockFirst().key == KEY
    }

    void 'generate presigned URL'() {
        when:
            String url = service.generatePresignedUrl(KEY, TOMORROW)
        then:
            url
            new URL(url).text == SAMPLE_CONTENT
    }

    void 'download file'() {
        when:
            File file = new File(tmp, 'bar.baz')

            service.getFile(KEY, file)
        then:
            file.exists()
            file.text == SAMPLE_CONTENT
    }

    void 'generate upload URL'() {
        when:
            String uploadUrl = service.generateUploadUrl(UPLOAD_KEY, TOMORROW)

            HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection()
            connection.doOutput = true
            connection.requestMethod = 'PUT'
            connection.setRequestProperty('User-Agent', 'Groovy')

            connection.outputStream.withWriter { Writer w ->
                w.write('Hello')
            }

        then:
            connection.responseCode == 200
            service.exists(UPLOAD_KEY)
    }

    void 'delete file'() {
        when:
            service.deleteFile(KEY)
            service.deleteFile(UPLOAD_KEY)
            service.deleteFile('bar/foo.txt')
        then:
            !service.exists(KEY)
            !service.exists(UPLOAD_KEY)
            !service.exists('bar/foo.txt')
    }

    void 'delete bucket'() {
        when:
            service.deleteBucket()
        then:
            !service.listBucketNames().contains(MY_BUCKET)
    }

}
