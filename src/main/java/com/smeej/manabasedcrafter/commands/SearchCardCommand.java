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

    private final SearchCardService searchCard;

    @Autowired
    public SearchCardCommand(SearchCardService searchCard) {
        this.searchCard = searchCard;
    }

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String cardName = event.getOption("card")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");

        // Log card name
        System.out.println("Searching for card: " + cardName);

        // Reply to the user with the card search result
        return searchCard.searchCardByName(cardName)
                .flatMap(card -> event.reply()
                        .withEphemeral(true)
                        .withContent(formatCardDetails(card)))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("No card found for: " + cardName);
                    return event.reply()
                            .withEphemeral(true)
                            .withContent("Card not found!");
                }));
    }

    private String formatCardDetails(Card card) {
        return String.format("**%s**\nType: %s\nMana Cost: %s\nText: %s",
                card.getName(),
                card.getType(),
                card.getManaCost(),
                card.getText());
    }
}
