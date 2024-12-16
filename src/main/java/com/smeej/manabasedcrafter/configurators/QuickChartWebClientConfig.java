package com.smeej.manabasedcrafter.configurators;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up a {@link WebClient} instance.
 * Provides a configured WebClient bean specifically for interacting with the QuickChart API.
 */
@Configuration
public class QuickChartWebClientConfig {

    private static final String HEADER_ACCEPT = "application/json";
    private static final String HEADER_USER_AGENT = "ManaBasedCrafterBot/1.0 (contact: djfleming.metis@gmail.com)";

    @Bean(name = "quickChartWebClient")
    public WebClient quickChartWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("https://quickchart.io")
                .defaultHeader("Accept", HEADER_ACCEPT)
                .defaultHeader("User-Agent", HEADER_USER_AGENT)
                .build();
    }
}
