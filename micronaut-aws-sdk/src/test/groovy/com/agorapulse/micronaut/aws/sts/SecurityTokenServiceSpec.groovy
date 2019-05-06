package com.agorapulse.micronaut.aws.sts

import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for SecurityTokenService.
 */
class SecurityTokenServiceSpec extends Specification {

    public static final String SESSION_NAME = 'session'
    public static final String ROLE_ARN = 'arn:::my-role'
    public static final int DURATION_SECONDS = 360
    public static final String EXTERNAL_ID = '123456789'

    AWSSecurityTokenService client = Mock()

    SecurityTokenService service = new DefaultSecurityTokenService(client)

    void 'assume role'() {
        given:
            AtomicReference<AssumeRoleRequest> requestReference = new AtomicReference<>()
        when:
            // tag::usage[]
            service.assumeRole('session', 'arn:::my-role', 360) {
                externalId = '123456789'
            }
            // end::usage[]
        then:
            requestReference.get()
            requestReference.get().externalId == EXTERNAL_ID
            requestReference.get().roleSessionName == SESSION_NAME
            requestReference.get().roleArn == ROLE_ARN
            requestReference.get().durationSeconds == DURATION_SECONDS

            1 * client.assumeRole(_) >> { AssumeRoleRequest request -> requestReference.set(request) }
    }

}
