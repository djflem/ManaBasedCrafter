package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Represents a command for searching Magic: The Gathering cards by name using the Scryfall API.
 * This command is implemented as a SlashCommand and is intended to be used within a Discord
 * bot application, allowing users to search for card details via a slash command interaction.
 * <p>
 * Key Features:
 * - Processes the `searchcard` slash command to search for a card by name.
 * - Validates user input for the card name to ensure it's not empty.
 * - Utilizes the ScryfallSearchCardService to query the card data.
 * - Responds to users with the card image URL or an error message if the card cannot be found.
 * - Introduces a short delay between the request and response to avoid API throttling.
 * - Handles exceptions and provides user-friendly error messages.
 * <p>
 * Primary Behaviors:
 * - Extracts the card name from the command input and URL-encodes it for the Scryfall API request.
 * - Processes API responses, validating the presence of a card image before replying.
 * - Logs errors and stack traces, ensuring proper error handling mechanisms are in place.
 * <p>
 * Requirements:
 * - The command depends on the ScryfallSearchCardService for API interaction.
 * - Command responses are managed using Mono types for reactive programming compatibility.
 * - Requires interaction events to contain a valid "cardname" option.
 */
@Component
public class SearchCardByNameCommand implements SlashCommand {

    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // Extracted constant for the delay duration
    private static final String DEFAULT_ERROR_MESSAGE = "An error occurred while processing your request. Please check the card name and try again.";

    private final ScryfallSearchCardService scryfallSearchCardService;

    public SearchCardByNameCommand(ScryfallSearchCardService scryfallSearchCardService) {
        this.scryfallSearchCardService = scryfallSearchCardService;
    }

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        try {
            String encodedCardName = extractCardName(event);

            return scryfallSearchCardService.searchCardByName(encodedCardName)
                    .flatMap(response -> handleCardResponse(response, event))
                    .delayElement(REQUEST_DELAY)
                    .onErrorResume(error -> handleError(event, error));
        } catch (IllegalArgumentException e) {
            return handleError(event, e); // Handle invalid input immediately
        }
    }

    private String extractCardName(ChatInputInteractionEvent event) {
        return event.getOption("cardname")
                .flatMap(option -> option.getValue().map(value -> URLEncoder.encode(value.asString(), StandardCharsets.UTF_8)))
                .orElseThrow(() -> new IllegalArgumentException("Card name cannot be empty."));
    }

    private Mono<Void> handleCardResponse(ScryfallResponse response, ChatInputInteractionEvent event) {
        Map<String, String> cardImageUris = response != null ? response.getImageUris() : null;

        if (cardImageUris != null && cardImageUris.containsKey("normal")) {
            return replyWithCardImage(event, cardImageUris.get("normal"));
        } else {
            return handleError(event, new IllegalArgumentException(DEFAULT_ERROR_MESSAGE));
        }
    }

    private Mono<Void> replyWithCardImage(ChatInputInteractionEvent event, String imageUrl) {
        return event.reply().withEphemeral(false).withContent(imageUrl);
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error during searchcard command: " + error.getMessage());
        error.printStackTrace(); // Log the full stack trace for debugging

        return event.reply()
                .withEphemeral(true)
                .withContent(DEFAULT_ERROR_MESSAGE);
    }
}
