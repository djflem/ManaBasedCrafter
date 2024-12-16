package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.services.ScryfallSearchCardByNameService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SearchCardsByFileCommand implements SlashCommand {

    private final ScryfallSearchCardByNameService scryfallSearchCardByNameService;

    public SearchCardsByFileCommand(ScryfallSearchCardByNameService scryfallSearchCardByNameService) {
        this.scryfallSearchCardByNameService = scryfallSearchCardByNameService;
    }

    @Override
    public String getName() {
        return "importdeck";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return null;
    }

    /*
    private String extractFileCardNames(ChatInputInteractionEvent event) {
        return event.getOption("textorcsvfile")
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

    private Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error: " + error.getMessage());
        return event.reply()
                .withEphemeral(true)
                .withContent("Card not found.");
    }
    */
}
