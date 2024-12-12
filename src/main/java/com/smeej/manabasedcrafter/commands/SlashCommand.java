package com.smeej.manabasedcrafter.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * A simple interface defining our slash command class contract.
 *  a getName() method to provide the case-sensitive name of the command.
 *  and a handle() method which will house all the logic for processing each command.
 */
public interface SlashCommand {

    /**
     * Retrieves the case-sensitive name of the command.
     *
     * @return the name of the command as a String
     */
    String getName();

    /**
     * Handles the logic for processing a specific slash command event.
     * This method is invoked when a ChatInputInteractionEvent is received,
     * and it is responsible for executing the associated command logic.
     *
     * @param event the ChatInputInteractionEvent that contains details of the slash command interaction
     * @return a Mono<Void> that completes when the command processing is finished
     */
    Mono<Void> handle(ChatInputInteractionEvent event);
}
