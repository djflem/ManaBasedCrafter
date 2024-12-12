package com.smeej.manabasedcrafter.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.smeej.manabasedcrafter.commands.SlashCommand;

import java.util.Collection;
import java.util.List;

/**
 * The SlashCommandListener class is responsible for listening to and handling Discord slash command events.
 * This class leverages a collection of SlashCommand implementations to manage and process slash command interactions.
 *
 * The listener subscribes to ChatInputInteractionEvent provided by a GatewayDiscordClient, filtering
 * and delegating events to the appropriate SlashCommand based on the command name.
 *
 * The class uses reactive streams to process commands and ensures all matching and handling logic
 * is encapsulated within the corresponding SlashCommand implementation.
 */
@Component
public class SlashCommandListener {

    private final Collection<SlashCommand> commands;

    public SlashCommandListener(List<SlashCommand> slashCommands, GatewayDiscordClient client) {
        commands = slashCommands;

        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.getName().equals(event.getCommandName()))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> command.handle(event));
    }
}
