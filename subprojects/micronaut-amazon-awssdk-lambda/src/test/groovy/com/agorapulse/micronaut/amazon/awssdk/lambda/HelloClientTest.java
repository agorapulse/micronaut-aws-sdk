package com.agorapulse.micronaut.amazon.awssdk.lambda;

import com.agorapulse.testing.fixt.Fixt;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipUtil;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.Runtime;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
