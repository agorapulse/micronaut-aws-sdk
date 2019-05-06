package com.agorapulse.micronaut.aws.sts;

import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface SecurityTokenService {

    default AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds) {
        return assumeRole(sessionName, roleArn, durationInSeconds, (p) -> { });
    }

    default AssumeRoleResult assumeRole(
        String sessionName,
        String roleArn,
        int durationInSeconds,
        @DelegatesTo(value = AssumeRoleRequest.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.amazonaws.services.securitytoken.model.AssumeRoleRequest")
            Closure additionParameters
    ) {
        return assumeRole(sessionName, roleArn, durationInSeconds, ConsumerWithDelegate.create(additionParameters));
    }

    /**
     * Create credentials for role assumption.
     * @param sessionName the name of the session
     * @param roleArn the role ARN
     * @param durationInSeconds the duration in seconds
     * @param additionParameters the additional parameters
     * @return the assumption result containing the provisioned credentials
     */
    AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds, Consumer<AssumeRoleRequest> additionParameters);

}
