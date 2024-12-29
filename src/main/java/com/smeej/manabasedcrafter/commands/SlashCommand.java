package com.smeej.manabasedcrafter.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * Represents a slash command interface that defines the structure for Discord slash command handling.
 * Implementations of this interface are responsible for managing specific command logic
 * and processing interaction events triggered within a Discord application.
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
    Mono<Void> handleCommand(ChatInputInteractionEvent event);

    /**
     * Handles errors that occur during the processing of a slash command interaction.
     * This method is responsible for logging the error and providing an appropriate
     * response to the user.
     *
     * @param event the ChatInputInteractionEvent that represents the slash command interaction
     * @param error the Throwable representing the error that occurred
     * @return a Mono<Void> that completes after sending an error response
     */
    Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error);
}
