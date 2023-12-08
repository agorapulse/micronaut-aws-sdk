/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.s3

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.TempDir
import spock.lang.Unroll

import jakarta.inject.Inject

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
    private static final String OTHER_BUCKET = 'otherbucket'
    private static final String SAMPLE_CONTENT = 'hello world!'
    private static final Date TOMORROW = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
    private static final String NO_SUCH_BUCKET = 'no-such-bucket'

    @TempDir File tmp

    @Inject S3Client amazonS3
    @Inject S3Presigner presigner
    // tag::setup[]
    @Inject SimpleStorageService service                                                // <3>
    // end::setup[]

    void 'new bucket'() {
        when:
            service.createBucket()
            service.createBucket(OTHER_BUCKET)
        then:
            service.listBucketNames().contains(MY_BUCKET)
    }

    void 'upload file'() {
        when:
            File file = new File(tmp, 'foo.txt')
            file.createNewFile()
            file.text = SAMPLE_CONTENT

            service.storeFile('bar/foo.txt', file) {
                acl ObjectCannedACL.PUBLIC_READ
            }
        then:
            service.exists('bar/foo.txt')
        and:
            !service.storeFile(NO_SUCH_BUCKET, 'bar/foo.txt', file)

        when:
            service.storeFile(OTHER_BUCKET, 'bar/other-foo.txt', file) {
                acl ObjectCannedACL.PUBLIC_READ
            }
        then:
            service.exists(OTHER_BUCKET, 'bar/other-foo.txt')

        when:
            service.storeFile('bar/second.txt', file)
        then:
            service.exists('bar/second.txt')
    }

    void 'upload content'() {
        expect:
            !blocking(service.listObjectSummaries('foo'))?.size()
        when:
            service.storeInputStream(KEY, new ByteArrayInputStream(SAMPLE_CONTENT.bytes)) {
                contentLength SAMPLE_CONTENT.size()
                contentType 'text/plain'
                contentDisposition 'bar.baz'
            }

        then:
            blocking(service.listObjectSummaries('foo')).key() == KEY

        when:
            service.storeInputStream(OTHER_BUCKET, KEY, new ByteArrayInputStream(SAMPLE_CONTENT.bytes)) {
                contentLength SAMPLE_CONTENT.size()
                contentType 'text/plain'
                contentDisposition 'bar.baz'
            }

        then:
            blocking(service.listObjectSummaries(OTHER_BUCKET, 'foo')).key() == KEY
    }

    void 'upload multipart'() {
        when:
            service.storeMultipartFile('mix/multi', new MockPartData('Hello')) {
                acl ObjectCannedACL.PUBLIC_READ
            }
            S3Object object = blocking(service.listObjectSummaries('mix'))
        then:
            object
            object.size() == 5
    }

    void 'upload multipart - other'() {
        when:
            service.storeMultipartFile(OTHER_BUCKET, 'mix/multi', new MockPartData('Hello')) {
                acl ObjectCannedACL.PUBLIC_READ
            }
            S3Object object = blocking(service.listObjectSummaries(OTHER_BUCKET, 'mix'))
        then:
            object
            object.size() == 5
    }

    @Unroll
    void 'move object created with canned acl #desiredAcl'() {
        when:
            String newKey = 'mix/moved-' + desiredAcl
            String oldKey = 'mix/to-be-moved-' + desiredAcl
            service.storeMultipartFile(oldKey, new MockPartData('Public')) {
                acl desiredAcl
                tagging(Tagging.builder().tagSet(Tag.builder().key('foo').value('bar').build()).build())
                contentDisposition 'attachment'
                contentLanguage 'en'
                metadata meta: 'test'
            }
            GetObjectAclResponse oldAcls = amazonS3.getObjectAcl { it.bucket(MY_BUCKET).key(oldKey) }
        and:
            service.moveObject(oldKey, MY_BUCKET, newKey)
        and:
            GetObjectResponse moved = service.getObject(newKey)
        then:
            moved
            moved.contentLength() == 6
            moved.contentType() == 'text/plain'
            moved.contentDisposition() == 'attachment'
            moved.metadata() == [meta: 'test']

            amazonS3.getObjectTagging { it.bucket(MY_BUCKET).key(newKey) }
                .tagSet()
                .any { it.key() == 'foo' && it.value() == 'bar' }

            !service.exists(oldKey)

        when:
            GetObjectAclResponse newAcls = amazonS3.getObjectAcl { it.bucket(MY_BUCKET).key(newKey) }
        then:
            newAcls.toString() == oldAcls.toString()
        and:
            !service.moveObject(oldKey, MY_BUCKET, newKey)
        where:
            desiredAcl << [
                ObjectCannedACL.PRIVATE,
                ObjectCannedACL.PUBLIC_READ,
                ObjectCannedACL.PUBLIC_READ_WRITE,
                ObjectCannedACL.AUTHENTICATED_READ,
                ObjectCannedACL.AWS_EXEC_READ,
                ObjectCannedACL.BUCKET_OWNER_READ,
                ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL,
            ]
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

        when:
            File file2 = new File(tmp, 'bar2.baz')
            service.getFile(KEY, file2.canonicalPath)
        then:
            file2.exists()
            file2.text == SAMPLE_CONTENT
    }

    void 'delete file'() {
        expect:
            // there is no way how to determine the file did not exist before
            service.deleteFile('xyz/no-such-file')
            // but the bucket must exist, otherwise an error is thrown
            !service.deleteFile(NO_SUCH_BUCKET, 'xyz/no-such-file')
        when:
            service.deleteFile(KEY)
            service.deleteFile('bar/foo.txt')
        then:
            !service.exists(KEY)
            !service.exists('bar/foo.txt')
            !service.exists('')
    }

    void 'delete files'() {
        expect:
            !service.deleteFiles(NO_SUCH_BUCKET, 'mix')
            service.getObjectSummary('mix')
            service.deleteFiles('mix')
            !blocking(service.listObjectSummaries('mix'))
        when:
            service.deleteFiles(NO_SUCH_BUCKET, 'mix/foo/bar/baz')
        then:
            thrown(IllegalArgumentException)
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

    void 'delete bucket'() {
        given:
            service.deleteFiles('')
        expect:
            !blocking(service.listObjectSummaries())
            !blocking(Flux.from(service.listObjects()).flatMap { r ->
                Flux.fromIterable(r.contents())
            })
        when:
            service.deleteBucket()
        then:
            !service.listBucketNames().contains(MY_BUCKET)
    }

    void 'error handling'() {
        expect:
            !service.exists(MY_BUCKET, KEY)
            !service.storeInputStream(KEY, new ByteArrayInputStream('Hello'.bytes))
            !service.storeMultipartFile(KEY, new MockPartData('Foo'))
    }

    private static <T> T blocking(Publisher<T> publisher) {
        return Flux.from(publisher).blockFirst()
    }

}
