package com.smeej.manabasedcrafter.services;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import reactor.core.publisher.Mono;

/**
 * The ScryfallService interface provides methods to interact with the Scryfall API.
 * It is designed to contract operations such as searching for cards by their names through implementation.
 */
public interface ScryfallService {

    /**
     * Searches for a card by its name using the Scryfall API.
     * This method returns a reactive Mono encapsulating the search results
     * in the form of a {@link ScryfallResponse}.
     *
     * @param cardName the name of the card to search for; must not be null or empty
     * @return a Mono containing the {@link ScryfallResponse} with details about the found card,
     *         or an empty Mono if no card is found
     */
    Mono<ScryfallResponse> searchCardByName(String cardName);
}