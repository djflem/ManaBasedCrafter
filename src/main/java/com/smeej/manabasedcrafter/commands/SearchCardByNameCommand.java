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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * This class represents a command for searching Magic: The Gathering cards by their name
 * using the Scryfall API. It implements the SlashCommand interface to handle specific
 * slash command logic within a Discord application.
 * <p>
 * Key Responsibilities:
 * - Extracts the card name from the slash command options.
 * - Utilizes the ScryfallSearchCardService to perform searches by card name and handle
 *   potential fallback scenarios.
 * - Processes successful responses by formatting and sending results back to the user.
 * - Handles both single-faced and double-faced card responses with proper formatting.
 * - Manages errors gracefully, ensuring the user is notified of any issues during command execution.
 * <p>
 * Designed Usage:
 * This command is executed when the user inputs the "searchcard" slash command within a
 * Discord server. It supports fuzzy name searches and retries failed requests
 * automatically according to a defined retry policy.
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
                        return scryfallSearchCardService.searchDoubleFacedCardByName(cardName)
                                .flatMap(fallbackResponse -> handleCardResponse(fallbackResponse, event))
                                .onErrorResume(error -> handleError(event, error));
                    })
                    .retryWhen(Retry.fixedDelay(3, REQUEST_DELAY))
                    .onErrorResume(error -> handleError(event, error));
        } catch (IllegalArgumentException error) {
            return handleError(event, error);
        }
    }

    private String extractCardNameOption(ChatInputInteractionEvent event) {
        return event.getOption("cardname")
                .flatMap(option -> option.getValue().map(value -> FileProcessingUtils.validateAndEncodeCardName(value.asString()))) // Sanitization
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.CARD_NAME_EMPTY));
    }

    private Mono<Void> handleCardResponse(ScryfallResponse response, ChatInputInteractionEvent event) {
        if (response == null) {
            return handleError(event, new IllegalArgumentException(ErrorMessages.API_RESPONSE_ERROR));
        }

        // Check if the card has multiple faces
        if (response.getCardFaces() != null && !response.getCardFaces().isEmpty()) {
            StringBuilder responseDoubleCardText = new StringBuilder();

            // Handle double faced cards
            for (Map<String, Object> face : response.getCardFaces()) {
                String faceName = (String) face.get("name");
                Map<String, String> faceImageUris = (Map<String, String>) face.get("image_uris");

                if (faceName != null && faceImageUris != null && faceImageUris.containsKey("normal")) {
                    String imageUrl = faceImageUris.get("normal");
                    responseDoubleCardText.append("  :  [").append(faceName).append("](").append(imageUrl).append(")");
                }
            }
            return event.reply()
                    .withEphemeral(false)
                    .withContent(responseDoubleCardText.toString().trim());
        }

        // Handle normal cards
        if (response.getImageUris() != null && response.getImageUris().containsKey("normal")) {
            String imageUrl = response.getImageUris().get("normal");
            String responseSingleCardText = "  :  [" + response.getName() + "](" + imageUrl + ")";
            return event.reply()
                    .withEphemeral(false)
                    .withContent(responseSingleCardText);
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
