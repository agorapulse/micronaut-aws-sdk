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
package com.agorapulse.micronaut.aws.ses;

import java.io.File;

/**
 * Attachment builder.
 */
public class TransactionalEmailAttachment {

    private String filename;
    private String filepath;
    private String mimeType;
    private String description = "";

    public TransactionalEmailAttachment filename(String filename) {
        this.filename = filename;
        return this;
    }

    public TransactionalEmailAttachment filepath(String filepath) {
        this.filepath = filepath;

        if (filepath != null) {
            File f = new File(filepath);
            if (f.exists()) {
                if (this.mimeType == null) {
                    this.mimeType = MimeType.mimeTypeFromFilename(f.getName());
                }
                if (this.filename == null) {
                    this.filename = f.getName();
                }
            }
        }

        return this;
    }

    public TransactionalEmailAttachment mimeType(String str) {
        this.mimeType = str;
        return this;
    }

    public TransactionalEmailAttachment description(String str) {
        this.description = str;
        return this;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getDescription() {
        return description;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "TransactionalEmailAttachment{" +
            "filename='" + filename + '\'' +
            ", filepath='" + filepath + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
    // CHECKSTYLE:ON
}
