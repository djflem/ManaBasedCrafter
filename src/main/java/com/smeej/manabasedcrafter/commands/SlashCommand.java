package com.smeej.manabasedcrafter.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * Represents a Discord Slash Command that can be used for processing user interactions with custom commands.
 * Implementations of this interface define specific command names and their logic upon user invocation.
 *
 * This interface facilitates:
 * - Retrieving the command name.
 * - Handling the logic upon activation by a user.
 * - Managing error handling for any exceptions during command processing.
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

    /**
     * Handles errors that occur during the processing of a Discord slash command interaction.
     * This method logs the error and sends a short response to the user indicating that
     * the operation was unsuccessful.
     *
     * @param event the ChatInputInteractionEvent that contains details of the user interaction
     * @param error the Throwable that represents the error encountered during command processing
     * @return a Mono<Void> that completes after sending the error message to the user
     */
    Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error);
}
