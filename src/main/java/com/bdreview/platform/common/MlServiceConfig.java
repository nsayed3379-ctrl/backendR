package com.bdreview.platform.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Base HTTP client pointed at the Python ML microservice (fake-review §14 + summary §15). */
@Configuration
public class MlServiceConfig {

    @Bean
    public WebClient mlServiceWebClient(@Value("${app.ml-service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
