package com.agorapulse.micronaut.amazon.awssdk.s3

import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.multipart.PartData

import java.nio.ByteBuffer

@CompileStatic
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
