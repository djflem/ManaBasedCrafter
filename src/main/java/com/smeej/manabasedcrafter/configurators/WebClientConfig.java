package com.smeej.manabasedcrafter.configurators;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for creating and configuring WebClient beans to interact with external APIs.
 * This class defines specific WebClient beans for interacting with the Scryfall and QuickChart APIs,
 * as well as a general-purpose WebClient bean for generic API interactions.
 * <p>
 * The headers for all WebClient beans include an Accept header specifying application/json
 * and a User-Agent header identifying the application.
 */
@Configuration
public class WebClientConfig {

    private static final String SCRYFALL_BASE_URL = "https://api.scryfall.com";
    private static final String QUICKCHART_BASE_URL = "https://quickchart.io";
    private static final String HEADER_ACCEPT = "application/json";
    private static final String HEADER_USER_AGENT = "ManaBasedCrafterBot/1.0 (contact: djfleming.metis@gmail.com)";

    @Bean(name = "scryfallWebClient")
    public WebClient scryfallWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(SCRYFALL_BASE_URL)
                .defaultHeader("Accept", HEADER_ACCEPT)
                .defaultHeader("User-Agent", HEADER_USER_AGENT)
                .build();
    }

    @Bean(name = "quickChartWebClient")
    public WebClient quickChartWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(QUICKCHART_BASE_URL)
                .defaultHeader("Accept", HEADER_ACCEPT)
                .defaultHeader("User-Agent", HEADER_USER_AGENT)
                .build();
    }

    @Bean(name = "generalWebClient")
    public WebClient generalWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .defaultHeader("Accept", HEADER_ACCEPT)
                .build();
    }
}
