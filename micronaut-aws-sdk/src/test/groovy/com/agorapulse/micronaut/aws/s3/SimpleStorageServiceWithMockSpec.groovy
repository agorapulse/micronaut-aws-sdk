package com.agorapulse.micronaut.aws.s3

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.Region
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.model.UploadResult
import io.micronaut.http.MediaType
import io.micronaut.http.multipart.PartData
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer

/**
 * Tests for simple storage service.
 */
class SimpleStorageServiceWithMockSpec extends Specification {

    private static final String BUCKET_NAME = 'bucket'
    private static final String REGION = 'eu-west-1'

    @Rule TemporaryFolder tmp

    AmazonS3 client = Mock(AmazonS3) {
        getUrl(_, _) >> { String bucket, String key ->
            return new URL("https://s3-eu-west-1.amazonaws.com/$bucket/$key")
        }

        getRegion() >> Region.fromValue(REGION)
        getRegionName() >> REGION
    }

    SimpleStorageService service = new DefaultSimpleStorageService(client, BUCKET_NAME)

    /**
     * Tests for deleteFile(String key)
     */
    void "Deleting a file "() {
        when:
            boolean deleted = service.deleteFile('key')

        then:
            deleted
            1 * client.deleteObject(BUCKET_NAME, 'key')
    }

    void "Deleting a file with service exception"() {
        when:
            boolean deleted = service.deleteFile('key')

        then:
            !deleted
            1 * client.deleteObject(_, _) >> {
                throw new AmazonS3Exception('some exception')
            }
    }

    void "Deleting a file with client exception"() {
        when:
            boolean deleted = service.deleteFile('key')

        then:
            !deleted
            1 * client.deleteObject(_, _) >> {
                throw new AmazonClientException('some exception')
            }
    }

    /**
     * Tests for deleteFiles(String prefix)
     */
    void "Deleting files"() {
        when:
            boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
            deleted
            1 * client.listObjects(BUCKET_NAME, 'dir/subdir/*') >> {
                Map<String, List> objectListing = [objectSummaries: []]
                3.times {
                    S3ObjectSummary summary = new S3ObjectSummary()
                    summary.key = "key$it"
                    objectListing.objectSummaries << summary
                }
                objectListing as ObjectListing
            }
            3 * client.deleteObject(BUCKET_NAME, _)
    }

    void "Deleting files with invalid prefix"() {
        when:
            boolean deleted = service.deleteFiles('prefix')

        then:
            thrown AssertionError
            !deleted
            0 * client._
    }

    void "Deleting files with service exception"() {
        when:
            boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
            !deleted

            1 * client.listObjects(_, _) >> {
                throw new AmazonS3Exception('some exception')
            }
    }

    void "Deleting files with client exception"() {
        when:
            boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
            !deleted
            1 * client.listObjects(_, _) >> {
                throw new AmazonClientException('some exception')
            }
    }

    /**
     * Tests for exists(String prefix)
     */
    void "Checking if a file exists"() {
        when:
            boolean exists = service.exists('key')

        then:
            exists
            1 * client.listObjects(BUCKET_NAME, 'key') >> {
                S3ObjectSummary summary = new S3ObjectSummary()
                summary.key = 'key'
                [objectSummaries: [summary]] as ObjectListing
            }
    }

    void "Checking if a file does not exists"() {
        when:
            boolean exists = service.exists('key')

        then:
            !exists
            1 * client.listObjects(BUCKET_NAME, 'key') >> {
                [] as ObjectListing
            }
    }

    void "Checking if a file exists with invalid key parameter"() {
        when:
            boolean exists = service.exists('')

        then:
            !exists
            0 * client._
    }

    void "Checking if a file exists with service exception"() {
        when:
            boolean exists = service.exists('prefix')

        then:
            !exists
            1 * client.listObjects(_, _) >> {
                throw new AmazonS3Exception('some exception')
            }
    }

    void "Checking if a file exists with client exception"() {
        when:
            boolean exists = service.exists('prefix')

        then:
            !exists
            1 * client.listObjects(BUCKET_NAME, _) >> {
                throw new AmazonClientException('some exception')
            }
    }

    /**
     * Tests for generatePresignedUrl(String key, Date expiration)
     */
    @SuppressWarnings('NoJavaUtilDate')
    void "Generating presigned url"() {
        when:
            String presignedUrl = service.generatePresignedUrl('key', new Date(System.currentTimeMillis() + 24 * 3600 * 1000))

        then:
            presignedUrl == 'http://some.domaine.com/some/path'
            1 * client.generatePresignedUrl(BUCKET_NAME, 'key', _) >> new URL('http://some.domaine.com/some/path')
    }

    InputStream mockInputStream() {
        new InputStream() {
            @Override
            int read() throws IOException {
                return 0
            }
        }
    }

    void "Storing file"() {
        given:
            File file = Mock(File)
            String path = 'filePrefix.txt'

        when:
            String url = service.storeFile(path, file)

        then:
            url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
            1 * client.putObject(_)
    }

    void "Storing file exception"() {
        given:
            File file = Mock(File)
            String path = 'filePrefix.txt'

        when:
            String url = service.storeFile(path, file)

        then:
            url == ''
            1 * client.putObject(_) >> { throw new AmazonS3Exception('failed') }
    }

    @Unroll
    void 'Build metadata for #type and #extension'() {
        when:
            ObjectMetadata metadata = service.buildMetadataFromType(type, ext, CannedAccessControlList.BucketOwnerRead)
        then:
            metadata.contentType == contentType
            metadata.contentDisposition == contentDisposition
        where:
            type    | ext   | contentType                       | contentDisposition
            'image' | 'jpg' | 'image/jpeg'                      | null
            'any'   | 'swf' | 'application/x-shockwave-flash'   | null
            'any'   | 'foo' | 'application/octet-stream'        | 'attachment'
            'csv'   | 'csv' | 'text/csv'                        | 'attachment'
    }

    void "Storing input"() {
        given:
            InputStream input = mockInputStream()
            String path = 'filePrefix.txt'

        when:
            ObjectMetadata metadata = service.buildMetadataFromType('file', 'txt')
            String url = service.storeInputStream(path, input, metadata)

        then:
            url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
            1 * client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing pdf input with private ACL"() {
        given:
            InputStream input = mockInputStream()
            String path = 'filePrefix.fileSuffix.pdf'

        when:
            ObjectMetadata metadata = service.buildMetadataFromType('pdf', 'pdf')
            String url = service.storeInputStream(path, input, metadata)

        then:
            url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
            1 * client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing image input"() {
        given:
            InputStream input = mockInputStream()
            String path = 'filePrefix.fileSuffix.jpg'

        when:
            ObjectMetadata metadata = service.buildMetadataFromType('image', 'jpg')
            String url = service.storeInputStream(path, input, metadata)

        then:
            url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
            1 * client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing flash input"() {
        given:
            InputStream input = mockInputStream()
            String path = 'filePrefix.fileSuffix.swf'

        when:
            ObjectMetadata metadata = service.buildMetadataFromType('flash', 'swf')
            String url = service.storeInputStream(path, input, metadata)

        then:
            url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
            1 * client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing input with service exception"() {
        given:
            InputStream input = mockInputStream()

        when:
            String url = service.storeInputStream('somePath', input, new ObjectMetadata())

        then:
            !url
            1 * client.putObject(BUCKET_NAME, _, _, _) >> {
                throw new AmazonS3Exception('some exception')
            }
    }

    void "Storing input with client exception"() {
        given:
            InputStream input = mockInputStream()

        when:
            String url = service.storeInputStream('somePath', input, new ObjectMetadata())

        then:
            !url
            1 * client.putObject(BUCKET_NAME, _, _, _) >> {
                throw new AmazonClientException('some exception')
            }
    }

    void 'transfer file'() {
        given:
            File file = tmp.newFile('transferred.txt')
            file.text = 'transferring...'
        when:
            Upload upload = service.transferFile('transferred.txt', file)
            UploadResult result = upload.waitForUploadResult()
        then:
            result

            1 * client.putObject(_) >> new PutObjectResult()
    }

    void 'new bucket'() {
        when:
            service.createBucket('new-bucket')
        then:
            1 * client.createBucket('new-bucket')
    }

    void 'delete bucket'() {
        when:
            service.deleteBucket('old-bucket')
        then:
            1 * client.deleteBucket('old-bucket')
    }

    void 'get file'() {
        given:
            File file = tmp.newFile('transferred.txt')
        when:
            service.getFile('transferred.txt', file.absolutePath)
        then:
            1 * client.getObject(_, _)

        when:
            service.getFile('transferred.txt', file)
        then:
            1 * client.getObject(_, _)
    }

    void 'list bucket names'() {
        when:
            List<String> bucketNames = service.listBucketNames()
        then:
            bucketNames == ['boo']

            1 * client.listBuckets() >> [new Bucket('boo')]
    }

    void 'list objects'() {
        when:
            ObjectListing listing = service.listObjects().blockingFirst()
        then:
            listing.bucketName == BUCKET_NAME

            1 * client.listObjects(BUCKET_NAME, '' ) >> new ObjectListing(bucketName: BUCKET_NAME)
    }

    void 'store multipart'() {
        when:
            PartData file = new MockPartData('some text')
            service.storeMultipartFile('stored.txt', file)
        then:
            1 * client.putObject(BUCKET_NAME, 'stored.txt', _, _)
    }

}

class MockPartData implements PartData {

    private final String text

    MockPartData(String text) {
        this.text = text
    }

    @Override
    InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(text.bytes)
    }

    @Override
    byte[] getBytes() throws IOException {
        return text.bytes
    }

    @Override
    ByteBuffer getByteBuffer() throws IOException {
        throw new UnsupportedOperationException('Not implemented')
    }

    @Override
    Optional<MediaType> getContentType() {
        return Optional.of(MediaType.TEXT_PLAIN_TYPE)
    }
}
