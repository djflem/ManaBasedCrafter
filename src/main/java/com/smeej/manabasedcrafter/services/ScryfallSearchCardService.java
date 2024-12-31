package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Service class for interacting with the Scryfall API to search for Magic: The Gathering cards by name.
 * This service uses a WebClient instance to perform HTTP calls to the Scryfall API.
 * <p>
 * Key Features:
 * - Provides method to perform card searches by a partial or exact card name.
 * - Supports fuzzy search functionality as per Scryfall API.
 * - Utilizes JSON parsing to map API responses to domain objects.
 * <p>
 * Intended Use:
 * - Designed for integration with external services or command-based applications that require card lookup functionality.
 * - Methods in this service are reactive, based on Project Reactor's Mono.
 */
@Service
public class ScryfallSearchCardService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // 100 ms delay

    private final WebClient scryfallWebClient;

    public ScryfallSearchCardService(@Qualifier("scryfallWebClient") WebClient scryfallWebClient) {
        this.scryfallWebClient = scryfallWebClient;
    }

    // WebClient is essential because data must be retrieved from an external source (Scryfall API).
    public Mono<ScryfallResponse> searchCardByName(String cardName) {
        return scryfallWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards/named")
                        .queryParam("fuzzy", cardName) // Add query param without encoding `?`
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, REQUEST_DELAY))
                .mapNotNull(this::parseSearchCardResponse);
    }

    public Mono<ScryfallResponse> searchCardByFuzzyName(String cardName) {
        return scryfallWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards/search")
                        .queryParam("q", "is:double-faced name:\"" + cardName + "\"") // Query for double-faced cards
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, REQUEST_DELAY))
                .mapNotNull(this::parseSearchCardResponse);
    }

    private ScryfallResponse parseSearchCardResponse(String json) {
        try {
            return new ObjectMapper().readValue(json, ScryfallResponse.class);
        } catch (Exception e) {
            LOGGER.error("Error parsing Scryfall API response: {}", e.getMessage(), e);
            return null;
        }
    }
}
