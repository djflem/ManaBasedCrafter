package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import com.smeej.manabasedcrafter.utilities.ErrorMessages;
import com.smeej.manabasedcrafter.utilities.FileProcessingUtils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // 100 ms delay

    private final ScryfallSearchCardService scryfallSearchCardService;

    public SearchCardByNameCommand(ScryfallSearchCardService scryfallSearchCardService) {
        this.scryfallSearchCardService = scryfallSearchCardService;
    }

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handleCommand(ChatInputInteractionEvent event) {
        try {
            String cardName = extractCardNameOption(event);

            return scryfallSearchCardService.searchCardByName(cardName)
                    .flatMap(response -> {
                        // If a response is found, handle it
                        if (response != null) {
                            return handleCardResponse(response, event);
                        }

                        // Otherwise, fallback to find the card by querying double-faced cards
                        return scryfallSearchCardService.searchCardByFuzzyName(cardName)
                                .flatMap(fallbackResponse -> handleCardResponse(fallbackResponse, event))
                                .onErrorResume(fallbackError -> handleError(event, fallbackError));
                    })
                    .delayElement(REQUEST_DELAY)
                    .onErrorResume(error -> handleError(event, error));
        } catch (IllegalArgumentException e) {
            return handleError(event, e);
        }
    }

    private String extractCardNameOption(ChatInputInteractionEvent event) {
        return event.getOption("cardname")
                .flatMap(option -> option.getValue().map(value -> FileProcessingUtils.validateAndEncodeCardName(value.asString())))
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.CARD_NAME_EMPTY));
    }

    private Mono<Void> handleCardResponse(ScryfallResponse response, ChatInputInteractionEvent event) {
        if (response == null) {
            return handleError(event, new IllegalArgumentException(ErrorMessages.API_RESPONSE_ERROR));
        }

        // Check if the card has multiple faces
        if (response.getCardFaces() != null && !response.getCardFaces().isEmpty()) {
            StringBuilder imageUris = new StringBuilder();

            for (Map<String, Object> face : response.getCardFaces()) {
                Map<String, String> faceImageUris = (Map<String, String>) face.get("image_uris");
                if (faceImageUris != null && faceImageUris.containsKey("normal")) {
                    imageUris.append(faceImageUris.get("normal")).append("\n");
                }
            }

            return event.reply()
                    .withEphemeral(false)
                    .withContent(imageUris.toString().trim());
        }

        // Handle normal cards
        if (response.getImageUris() != null && response.getImageUris().containsKey("normal")) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent(response.getImageUris().get("normal"));
        }

        // Handle case where no image is found
        return handleError(event, new IllegalArgumentException(ErrorMessages.API_RESPONSE_ERROR));
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        LOGGER.error("Error in command [{}]: {}", getName(), error.getMessage(), error);
        return event.reply()
                .withEphemeral(true)
                .withContent(ErrorMessages.GENERIC_ERROR);
    }
}
