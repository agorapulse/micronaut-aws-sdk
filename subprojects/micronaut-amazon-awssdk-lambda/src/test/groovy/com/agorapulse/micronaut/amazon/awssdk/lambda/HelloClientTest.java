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
package com.agorapulse.micronaut.amazon.awssdk.lambda;

import com.agorapulse.testing.fixt.Fixt;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipUtil;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.Runtime;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;

@MicronautTest                                                                          // <1>
public class HelloClientTest {

    private static final Fixt FIXT = Fixt.create(AbstractClientSpec.class);             // <2>

    @TempDir private File tmp;
    @Inject private LambdaClient lambda;                                                // <3>
    @Inject private HelloClient client;                                                 // <4>

    @BeforeEach
    public void setupSpec() {
        prepareHelloFunction();                                                         // <5>
    }

    @Test
    public void invokeFunction() {
        HelloResponse result = client.hello("Vlad");                                    // <6>
        Assertions.assertEquals("Hello Vlad", result.getMessage());                     // <7>
    }

    private void prepareHelloFunction() {
        boolean alreadyExists = lambda.listFunctions()
            .functions()
            .stream()
            .anyMatch(fn -> "HelloFunction".equals(fn.functionName()));

        if (alreadyExists) {
            return;
        }

        File functionDir = new File(tmp, "HelloFunction");
        functionDir.mkdirs();

        FIXT.copyTo("HelloFunction", functionDir);

        File functionArchive = new File(tmp, "function.zip");
        ZipUtil.pack(functionDir, functionArchive);

        lambda.createFunction(create -> create.functionName("HelloFunction")
            .runtime(Runtime.NODEJS16_X)
            .role("HelloRole")
            .handler("index.handler")
            .environment(e ->
                e.variables(Collections.singletonMap("MICRONAUT_ENVIRONMENTS", "itest"))//<8>
            )
            .code(code -> {
                try {
                    InputStream archiveStream = Files.newInputStream(functionArchive.toPath());
                    SdkBytes archiveBytes = SdkBytes.fromInputStream(archiveStream);
                    code.zipFile(archiveBytes);
                } catch (IOException e) {
                    throw new IllegalStateException(
                        "Failed to create function from archive " + functionArchive, e
                    );
                }
            })
            .build());
    }

}
