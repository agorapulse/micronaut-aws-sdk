/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.gcp

import com.google.auth.oauth2.GoogleCredentials
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

class GcpCredentialsFactorySpec extends Specification {

    void 'default credentials bean is created from flat gcp.* properties'() {
        given:
            ApplicationContext ctx = ApplicationContext.run([
                    'aws.region'           : 'us-east-1',
                    'aws.accessKeyId'      : 'test-key',
                    'aws.secretKey'        : 'test-secret',
                    'gcp.project-number'   : '123456789012',
                    'gcp.workload-pool'    : 'aws-pool',
                    'gcp.workload-provider': 'aws-provider',
                    'gcp.service-account'  : 'sa@p.iam.gserviceaccount.com',
            ])

        expect:
            ctx.getBean(GoogleCredentials) != null
            ctx.getBean(GcpWorkloadIdentityConfiguration).projectNumber == '123456789012'

        cleanup:
            ctx?.close()
    }

    void 'no credentials bean is created when nothing is configured'() {
        given:
            ApplicationContext ctx = ApplicationContext.run([
                    'aws.region'     : 'us-east-1',
                    'aws.accessKeyId': 'test-key',
                    'aws.secretKey'  : 'test-secret',
            ])

        expect:
            !ctx.containsBean(GoogleCredentials)
            !ctx.containsBean(GcpWorkloadIdentityConfiguration)

        cleanup:
            ctx?.close()
    }

    void 'named credentials produce one bean per gcp.credentials entry, with default as primary'() {
        given:
            ApplicationContext ctx = ApplicationContext.run([
                    'aws.region'                                  : 'us-east-1',
                    'aws.accessKeyId'                             : 'test-key',
                    'aws.secretKey'                               : 'test-secret',
                    'gcp.credentials.default.project-number'      : '111111111111',
                    'gcp.credentials.default.workload-pool'       : 'aws-pool',
                    'gcp.credentials.default.workload-provider'   : 'aws-provider',
                    'gcp.credentials.default.service-account'     : 'default@p.iam.gserviceaccount.com',
                    'gcp.credentials.analytics.project-number'    : '222222222222',
                    'gcp.credentials.analytics.workload-pool'     : 'aws-pool',
                    'gcp.credentials.analytics.workload-provider' : 'aws-provider',
                    'gcp.credentials.analytics.service-account'   : 'analytics@p.iam.gserviceaccount.com',
            ])

        expect: 'no flat default bean when gcp.project-number is not set'
            !ctx.containsBean(DefaultGcpWorkloadIdentityConfiguration)

        and: 'one named configuration bean per entry'
            ctx.getBeansOfType(GcpWorkloadIdentityConfiguration).size() == 2
            ctx.getBean(GcpWorkloadIdentityConfiguration, Qualifiers.byName('default')).projectNumber == '111111111111'
            ctx.getBean(GcpWorkloadIdentityConfiguration, Qualifiers.byName('analytics')).projectNumber == '222222222222'

        and: 'one GoogleCredentials bean per entry, with default resolvable without a qualifier'
            ctx.getBeansOfType(GoogleCredentials).size() == 2
            ctx.getBean(GoogleCredentials) != null
            ctx.getBean(GoogleCredentials, Qualifiers.byName('default')) != null
            ctx.getBean(GoogleCredentials, Qualifiers.byName('analytics')) != null

        cleanup:
            ctx?.close()
    }

    void 'flat default and named entries can coexist'() {
        given:
            ApplicationContext ctx = ApplicationContext.run([
                    'aws.region'                                 : 'us-east-1',
                    'aws.accessKeyId'                            : 'test-key',
                    'aws.secretKey'                              : 'test-secret',
                    'gcp.project-number'                         : '111111111111',
                    'gcp.workload-pool'                          : 'aws-pool',
                    'gcp.workload-provider'                      : 'aws-provider',
                    'gcp.service-account'                        : 'flat@p.iam.gserviceaccount.com',
                    'gcp.credentials.analytics.project-number'   : '222222222222',
                    'gcp.credentials.analytics.workload-pool'    : 'aws-pool',
                    'gcp.credentials.analytics.workload-provider': 'aws-provider',
                    'gcp.credentials.analytics.service-account'  : 'analytics@p.iam.gserviceaccount.com',
            ])

        expect:
            ctx.containsBean(DefaultGcpWorkloadIdentityConfiguration)
            ctx.getBeansOfType(GcpWorkloadIdentityConfiguration).size() == 2
            ctx.getBean(GcpWorkloadIdentityConfiguration, Qualifiers.byName('default')).projectNumber == '111111111111'
            ctx.getBean(GcpWorkloadIdentityConfiguration, Qualifiers.byName('analytics')).projectNumber == '222222222222'
            ctx.getBeansOfType(GoogleCredentials).size() == 2

        cleanup:
            ctx?.close()
    }

}
