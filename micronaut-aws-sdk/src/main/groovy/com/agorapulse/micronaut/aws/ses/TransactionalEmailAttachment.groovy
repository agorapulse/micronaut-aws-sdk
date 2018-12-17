package com.agorapulse.micronaut.aws.ses

import groovy.transform.CompileStatic
import groovy.transform.ToString

@ToString
@CompileStatic
class TransactionalEmailAttachment {
    String filename
    String filepath
    String mimeType
    String description = ''

    @SuppressWarnings('ConfusingMethodName')
    void filename(String str) {
        this.filename = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void filepath(String str) {
        this.filepath = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void mimeType(String str) {
        this.mimeType = str
    }

    @SuppressWarnings('ConfusingMethodName')
    void description(String str) {
        this.description = str
    }

    @SuppressWarnings('JavaIoPackageAccess')
    String getMimeType() {
        if ( !this.mimeType && this.filepath ) {
            def f = new File(this.filepath)
            if ( f.exists() ) {
                this.mimeType = MimeType.mimeTypeFromFilename(f.name)
            }
        }

        this.mimeType
    }

    @SuppressWarnings('JavaIoPackageAccess')
    String getFilename() {
        if ( !this.filename && this.filepath ) {
            def f = new File(this.filepath)
            if ( f.exists() ) {
                this.filename = f.name
            }
        }

        this.filename
    }
}
