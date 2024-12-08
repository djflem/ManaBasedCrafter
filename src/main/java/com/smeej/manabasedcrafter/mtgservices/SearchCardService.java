package com.smeej.manabasedcrafter.mtgservices;

import io.magicthegathering.javasdk.api.CardAPI;
import io.magicthegathering.javasdk.resource.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SearchCardService {

    private static final Logger logger = LoggerFactory.getLogger(SearchCardService.class);

    /**
     * Searches for a Magic: The Gathering card by name.
     *
     * @param cardName The name of the card to search for.
     * @return An Optional containing the Card if found, or empty if not found.
     */
    public Mono<Card> searchCardByName(String cardName) {

        try {
            // Fetch all cards matching the name
            List<Card> cards = CardAPI.getAllCards(List.of(cardName));

            // Filter for exact name match (ignoring case)
            return cards.stream()
                    .filter(card -> card.getName().equalsIgnoreCase(cardName))
                    .findFirst()
                    .map(Mono::just)
                    .orElseGet(Mono::empty);
        } catch (Exception e) {
            logger.error("Error occurred while searching card by name: {}", e.getMessage());
            return Mono.empty(); // Return an empty Mono in case of an error
        }
    }
}