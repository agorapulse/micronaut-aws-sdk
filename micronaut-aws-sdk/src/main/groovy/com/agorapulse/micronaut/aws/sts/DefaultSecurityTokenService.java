package com.agorapulse.micronaut.aws.sts;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.util.function.Consumer;

@Singleton
@Requires(classes = AWSSecurityTokenService.class)
public class DefaultSecurityTokenService implements SecurityTokenService {

    private final AWSSecurityTokenService client;

    public DefaultSecurityTokenService(AWSSecurityTokenService client) {
        this.client = client;
    }

    @Override
    public AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds, Consumer<AssumeRoleRequest> additionParameters) {
        AssumeRoleRequest request = new AssumeRoleRequest()
            .withRoleSessionName(sessionName)
            .withRoleArn(roleArn)
            .withDurationSeconds(durationInSeconds);

        additionParameters.accept(request);

        return client.assumeRole(request);
    }
}
