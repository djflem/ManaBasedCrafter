package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.mtgservices.SearchCardService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import io.magicthegathering.javasdk.resource.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SearchCardCommand implements SlashCommand {

    @Autowired
    private SearchCardService searchCardService;

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String searchedCardName = getCardNameFromEvent(event);

        System.out.println("Searching for card: " + searchedCardName);

        return searchCardService.searchCardByName(searchedCardName)
                .flatMap(chosenCard -> replyToEvent(event, formatCardDetails(chosenCard)))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("No card found for: " + searchedCardName);
                    return replyToEvent(event, "Card not found!");
                }));
    }

    private String getCardNameFromEvent(ChatInputInteractionEvent event) {
        return event.getOption("card")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }

    private Mono<Void> replyToEvent(ChatInputInteractionEvent event, String content) {
        return event.reply()
                .withEphemeral(true)
                .withContent(content);
    }

    private String formatCardDetails(Card card) {
        return String.format("**%s**\nType: %s\nMana Cost: %s\nText: %s",
                card.getName(),
                card.getType(),
                card.getManaCost(),
                card.getText());
    }
}
