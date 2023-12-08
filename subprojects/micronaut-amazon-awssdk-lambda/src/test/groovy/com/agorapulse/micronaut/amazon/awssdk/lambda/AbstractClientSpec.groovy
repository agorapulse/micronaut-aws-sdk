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
package com.agorapulse.micronaut.amazon.awssdk.lambda

import com.agorapulse.testing.fixt.Fixt
import org.zeroturnaround.zip.ZipUtil
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.Runtime
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import jakarta.inject.Inject

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractClientSpec extends Specification {

    @Shared Fixt fixt = Fixt.create(AbstractClientSpec)

    @TempDir File tmp

    @Inject LambdaClient lambda

    void setup() {
        prepareHelloFunction()
    }

    @SuppressWarnings('ImplicitClosureParameter')
    private void prepareHelloFunction() {
        boolean alreadyExists = lambda.listFunctions().functions().any {
            it.functionName() == 'HelloFunction'
        }

        if (alreadyExists) {
            return
        }

        File functionDir = new File(tmp, 'HelloFunction')
        functionDir.mkdirs()

        fixt.copyTo('HelloFunction', functionDir)

        File functionArchive = new File(tmp, 'function.zip')
        ZipUtil.pack(functionDir, functionArchive)

        lambda.createFunction {
            it.functionName('HelloFunction')
              .runtime(Runtime.NODEJS16_X)
              .role('HelloRole')
              .handler('index.handler')
              .code { it.zipFile(SdkBytes.fromByteArray(functionArchive.bytes)) }
              .environment { it.variables(MICRNOAUT_ENVIRONMENT: 'itest') }
              .build()
        }
    }

}
