package com.nexus.investment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Configures a reusable WebClient instance.
     * This client can be used for making remote HTTP calls to other microservices.
     * * @param builder The default WebClient.Builder provided by Spring Boot.
     * @return A configured WebClient instance.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // You can customize the WebClient here, for example, setting a base URL,
        // default headers, or timeouts.

        // Example: Setting a base URL for a hypothetical Auth service
        return builder
                // .baseUrl("http://funder-auth-service-url/")
                // .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}