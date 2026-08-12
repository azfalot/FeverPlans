package com.fever.plans.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "provider")
public record ProviderProperties(
        String url,
        Duration connectTimeout,
        Duration readTimeout,
        Duration syncDelay) {
}
