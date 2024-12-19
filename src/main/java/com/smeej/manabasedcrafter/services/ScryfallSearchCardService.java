package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for searching Magic: The Gathering cards via the Scryfall API.
 *
 * This service interacts with the Scryfall API using a WebClient to perform searches
 * based on card names. It retrieves detailed card data from the API and parses the
 * data into a structured response object.
 *
 * Responsibilities:
 * - Provides a method for searching for cards by name with fuzzy matching.
 * - Converts the JSON response from the Scryfall API into a ScryfallResponse object.
 *
 * Core Functionality:
 * - `searchCardByName(String cardName)`: Searches the Scryfall API for a card matching
 *   the provided name and attempts fuzzy matching in cases where the name may not be exact.
 * - Handles JSON deserialization through Jackson's ObjectMapper.
 * - Ensures reliable API calls and manages potential JSON parsing errors.
 *
 * Dependencies:
 * - WebClient: Configured for communicating with the Scryfall API.
 *
 * The methods in this service are designed for use in applications that require
 * interaction with Magic: The Gathering card data, such as Discord bot commands
 * or other integrations.
 */
@Service
public class ScryfallSearchCardService {

    private final WebClient scryfallWebClient;

    public ScryfallSearchCardService(@Qualifier("scryfallWebClient") WebClient scryfallWebClient) {
        this.scryfallWebClient = scryfallWebClient;
    }

    // WebClient is essential because data must be retrieved from an external source (Scryfall API).
    public Mono<ScryfallResponse> searchCardByName(String cardName) {
        return scryfallWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/cards/named")
                        .queryParam("fuzzy", cardName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSearchCardResponse);
    }

    private ScryfallResponse parseSearchCardResponse(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, ScryfallResponse.class);
        } catch (Exception e) {
            System.err.println("Error mapping JSON: " + e.getMessage());
            return null;
        }
    }
}
