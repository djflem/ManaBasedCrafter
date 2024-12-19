package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * The SearchCardByNameCommand class implements the SlashCommand interface
 * and defines the functionality for a "searchcard" slash command, which
 * allows users to search for a Magic: The Gathering card by its name using
 * the Scryfall API.
 *
 * This command retrieves the card image or sends an appropriate error message
 * if the card cannot be found or if no image is available. The command logic
 * is executed asynchronously, and any errors encountered during execution are
 * handled gracefully.
 *
 * The primary dependencies and methods are described below:
 *
 * Dependencies:
 * - ScryfallSearchCardService: A service responsible for making requests to
 *   the Scryfall API and retrieving card data.
 *
 * Key Methods:
 * - getName(): Returns the name of the slash command, which is "searchcard".
 * - handle(ChatInputInteractionEvent): Processes the incoming slash command event
 *   by extracting the card name and retrieving the card information from
 *   the Scryfall API.
 * - extractCardName(ChatInputInteractionEvent): Extracts the card name entered
 *   by the user in the slash command input.
 * - handleCardResponse(ScryfallResponse, ChatInputInteractionEvent): Handles
 *   the response from the Scryfall API, sending the card's image if available
 *   or an appropriate message if not.
 * - handleError(ChatInputInteractionEvent, Throwable): Handles errors that
 *   occur during the command execution, responding with a suitable error message.
 */
@Component
public class SearchCardByNameCommand implements SlashCommand {

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
        String cardName = extractCardName(event);

        return scryfallSearchCardService.searchCardByName(cardName)
                .flatMap(response -> handleCardResponse(response, event))
                .onErrorResume(error -> handleError(event, error));
    }

    private String extractCardName(ChatInputInteractionEvent event) {
        return event.getOption("cardname")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }

    private Mono<Void> handleCardResponse(ScryfallResponse response, ChatInputInteractionEvent event) {
        if (response == null) {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Error parsing card data.");
        }

        Map<String, String> imageUris = response.getImageUris();

        if (imageUris != null && imageUris.containsKey("normal")) {
            return event.reply()
                    .withEphemeral(false)
                    .withContent(imageUris.get("normal"));
        } else {
            return event.reply()
                    .withEphemeral(true)
                    .withContent("No image available for this card.");
        }
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error: " + error.getMessage());
        return event.reply()
                .withEphemeral(true)
                .withContent("Card not found.");
    }
}
