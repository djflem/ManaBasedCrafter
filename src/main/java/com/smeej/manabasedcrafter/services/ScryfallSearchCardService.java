package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.utilities.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Provides services for searching Magic: The Gathering cards using the Scryfall API.
 * This class interacts with Scryfall to perform card lookups based on a card's name
 * with optional fuzzy matching or searches for cards with specific characteristics,
 * such as being double-faced.
 * <p>
 * The service uses a WebClient instance to communicate with the Scryfall API.
 * Rate-limiting and retry strategies are embedded to handle network issues
 * and ensure compliance with the API usage guidelines.
 * <p>
 * Functionality:
 * - Search for cards by name, allowing fuzzy matches.
 * - Search specifically for double-faced cards by name.
 * - Parses API responses into domain-specific objects for further processing.
 * <p>
 * Logging is used to capture any errors or issues encountered during the API interaction.
 * The service enforces a fixed delay between retry attempts for failed requests
 * and gracefully handles JSON parsing errors.
 * <p>
 * This service is typically used in command execution flows where card-related data
 * from Scryfall is needed.
 */
@Service
public class ScryfallSearchCardService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // 100 ms delay

    private final WebClient scryfallWebClient;

    public ScryfallSearchCardService(@Qualifier("scryfallWebClient") WebClient scryfallWebClient) {
        this.scryfallWebClient = scryfallWebClient;
    }

    public Mono<ScryfallResponse> searchCardByName(String cardName) {
        return scryfallWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards/named")
                        .queryParam("fuzzy", cardName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, REQUEST_DELAY))
                .mapNotNull(this::parseSearchCardResponse);
    }

    public Mono<ScryfallResponse> searchDoubleFacedCardByName(String cardName) {
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
            LOGGER.error(ErrorMessages.API_RESPONSE_ERROR + "{}", e.getMessage(), e);
            return null;
        }
    }
}
