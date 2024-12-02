package com.smeej.manabasedcrafter.listeners;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public abstract class MessageListener {
    private String author = "UNKNOWN";

    public Mono<Void> processMessage(final Message eventMessage) {
        return Mono.just(eventMessage)
                .filter(message -> {
                    final Boolean isNotBot = message.getAuthor()
                            .map(user -> !user.isBot())
                            .orElse(false);
                    if (isNotBot) {
                        message.getAuthor().ifPresent(user -> author = user.getUsername());
                    }
                    return isNotBot;
                })
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage(String.format("Hello '$s'", author)))
                .then();
    }
}
