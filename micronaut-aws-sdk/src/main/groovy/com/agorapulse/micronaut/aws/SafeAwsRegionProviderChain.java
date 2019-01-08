package com.agorapulse.micronaut.aws;

import com.amazonaws.regions.AwsRegionProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Region provider chain which throws no exception but returns null instead.
 */
class SafeAwsRegionProviderChain extends AwsRegionProvider {

    private static final Log LOG = LogFactory.getLog(SafeAwsRegionProviderChain.class);

    private final List<AwsRegionProvider> providers;

    public SafeAwsRegionProviderChain(AwsRegionProvider... providers) {
        this.providers = new ArrayList<>(providers.length);
        Collections.addAll(this.providers, providers);
    }

    @Override
    public String getRegion(){
        for (AwsRegionProvider provider : providers) {
            try {
                final String region = provider.getRegion();
                if (region != null) {
                    return region;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                LOG.info("Unable to load region from " + provider.toString() + ": " + e.getMessage());
            }
        }
        return null;
    }
}
