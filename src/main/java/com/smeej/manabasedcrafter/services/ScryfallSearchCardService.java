package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
