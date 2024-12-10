package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.mtgservices.SearchCardService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
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

        String searchedCardName = event.getOption("cardname")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        System.out.println("Searching for card: " + searchedCardName);


        return searchCardService.searchCardByName(searchedCardName)
            .flatMap(card ->
                event.reply()
                        .withEphemeral(true)
                        .withContent(SearchCardService.formatCardDetails(card))
            )

            // If not found
            .onErrorResume(e -> {
                System.out.println("No card found for: " + searchedCardName);
                return event.reply()
                        .withEphemeral(true)
                        .withContent("Card not found!");
            });
    }
}
