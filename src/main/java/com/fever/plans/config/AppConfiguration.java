package com.fever.plans.config;

import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
class AppConfiguration {
    @Bean
    HttpClient providerHttpClient(ProviderProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
