package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.SearchCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementation of the {@link ScryfallService} interface to interact with the Scryfall API.
 * This service provides functionality to search for Magic: The Gathering cards.
 */
@Service
public class ScryfallServiceImpl implements ScryfallService {

    private final WebClient webClient;

    public ScryfallServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<SearchCardResponse> searchCardByName(String cardName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/cards/named")
                        .queryParam("fuzzy", cardName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseSearchCardResponse);
    }

    private SearchCardResponse parseSearchCardResponse(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, SearchCardResponse.class);
        } catch (Exception e) {
            System.err.println("Error mapping JSON: " + e.getMessage());
            return null;
        }
    }
}