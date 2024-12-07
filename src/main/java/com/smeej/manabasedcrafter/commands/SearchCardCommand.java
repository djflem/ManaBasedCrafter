package com.smeej.manabasedcrafter.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SearchCardCommand implements SlashCommand {
    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String card = event.getOption("card")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        return event.reply()
                // searchCard using "card" value
                .withEphemeral(true)
                .withContent("Searching MtG database for: " + card + "...");
    }
}
