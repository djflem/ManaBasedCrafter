package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.SearchCardResponse;
import com.smeej.manabasedcrafter.services.ScryfallService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * A component class implementing the SlashCommand interface.
 * This class defines the logic for handling the "searchcard" command, allowing users to search for
 * Magic: The Gathering cards by their names using the Scryfall API.
 *
 * The class interacts with the ScryfallService to execute a search operation,
 * processes the results, and sends an appropriate response back to the event channel.
 *
 * Responsibilities:
 * - Extract the card name from the provided event inputs.
 * - Perform an external query using ScryfallService to search for a card by its name.
 * - Handle the search response by replying with the card's image link or an error message if the card is not found.
 * - Manage errors that occur during the search process or result handling.
 *
 * Methods:
 * - getName: Returns the name of the slash command ("searchcard").
 * - handle: Manages the workflow of extracting the card name, querying the card, and handling the response.
 * - extractCardName: Retrieves the card name input specified in the command event.
 * - handleCardResponse: Processes the search response, constructing an appropriate reply with the card image or an error message.
 * - handleError: Sends a fallback response to the user in case of errors during processing.
 *
 * Dependencies:
 * This class relies on an instance of ScryfallService to perform the card search operation.
 */
@Component
public class SearchCardByNameCommand implements SlashCommand {

    private final ScryfallService scryfallService;

    public SearchCardByNameCommand(ScryfallService scryfallService) {
        this.scryfallService = scryfallService;
    }

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String cardName = extractCardName(event);

        return scryfallService.searchCardByName(cardName)
                .flatMap(response -> handleCardResponse(response, event))
                .onErrorResume(error -> handleError(event, error));
    }

    private String extractCardName(ChatInputInteractionEvent event) {
        return event.getOption("cardname")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }

    private Mono<Void> handleCardResponse(SearchCardResponse response, ChatInputInteractionEvent event) {
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

    private Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error: " + error.getMessage());
        return event.reply()
                .withEphemeral(true)
                .withContent("Card not found.");
    }
}
