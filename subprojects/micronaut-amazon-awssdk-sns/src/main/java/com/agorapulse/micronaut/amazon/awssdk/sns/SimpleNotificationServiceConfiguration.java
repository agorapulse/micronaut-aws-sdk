package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;
import io.micronaut.context.env.Environment;

/**
 * Default simple queue service configuration.
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    protected SimpleNotificationServiceConfiguration(String prefix, Environment environment) {
        ios = forPlatform(prefix, "ios", environment);
        iosSandbox = forPlatform(prefix, "iosSandbox", environment);
        android = forPlatform(prefix, "android", environment);
        amazon = forPlatform(prefix, "amazon", environment);
    }

    @SuppressWarnings("DuplicateStringLiteral")
    private static Application forPlatform(final String prefix, final String platform, final Environment environment) {
        return new Application(environment.get(prefix + "." + platform + ".arn", String.class).orElseGet(() ->
            environment.get(prefix + "." + platform + ".applicationArn", String.class).orElse(null)
       ));
    }

    public final Application getIos() {
        return ios;
    }

    public final Application getIosSandbox() {
        return iosSandbox;
    }

    public final Application getAndroid() {
        return android;
    }

    public final Application getAmazon() {
        return amazon;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    private final Application ios;
    private final Application iosSandbox;
    private final Application android;
    private final Application amazon;
    private String topic = "";

    public static class Application {

        public Application(String arn) {
            this.arn = arn;
        }

        public final String getArn() {
            return arn;
        }

        private final String arn;

    }
}
