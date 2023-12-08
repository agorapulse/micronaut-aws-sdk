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
package com.agorapulse.micronaut.aws.lambda

import com.agorapulse.testing.fixt.Fixt
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.CreateFunctionRequest
import com.amazonaws.services.lambda.model.Environment
import com.amazonaws.services.lambda.model.FunctionCode
import com.amazonaws.services.lambda.model.Runtime
import groovy.transform.CompileStatic
import org.zeroturnaround.zip.ZipUtil
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import jakarta.inject.Inject
import java.nio.ByteBuffer

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractClientSpec extends Specification {

    @Shared Fixt fixt = Fixt.create(AbstractClientSpec)

    @TempDir File tmp

    @Inject AWSLambda lambda

    void setup() {
        prepareHelloFunction()
    }

    @CompileStatic
    @SuppressWarnings('ImplicitClosureParameter')
    private void prepareHelloFunction() {
        boolean alreadyExists = lambda.listFunctions().functions.any {
            it.functionName == 'HelloFunction'
        }

        if (alreadyExists) {
            return
        }

        File functionDir = new File(tmp, 'HelloFunction')
        functionDir.mkdirs()

        fixt.copyTo('HelloFunction', functionDir)

        File functionArchive = new File(tmp, 'function.zip')
        ZipUtil.pack(functionDir, functionArchive)

        CreateFunctionRequest request = new CreateFunctionRequest()
            .withFunctionName('HelloFunction')
            .withRuntime(Runtime.Nodejs16X)
            .withRole('HelloRole')
            .withHandler('index.handler')
            .withCode(new FunctionCode().withZipFile(ByteBuffer.wrap(functionArchive.bytes)))
            .withEnvironment(new Environment(variables: [MICRONAUT_ENVIRONMENTS: 'itest']))

        lambda.createFunction(request)
    }

}
