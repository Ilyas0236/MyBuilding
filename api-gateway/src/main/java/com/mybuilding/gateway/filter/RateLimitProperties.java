package com.mybuilding.gateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mybuilding.gateway.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int capacity = 20;
    private int refillTokens = 20;
    private int refillSeconds = 15;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    public int getRefillSeconds() {
        return refillSeconds;
    }

    public void setRefillSeconds(int refillSeconds) {
        this.refillSeconds = refillSeconds;
    }
}
