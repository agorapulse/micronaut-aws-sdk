package com.agorapulse.micronaut.amazon.awssdk.localstack;

import io.micronaut.context.annotation.ConfigurationProperties;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("localstack")
public class LocalstackContainerConfiguration {

    // taken from LocalstackContainer defaults which are private constants
    private String image = "localstack/localstack";
    private String tag = "0.11.2";

    private List<String> services = new ArrayList<>();

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
