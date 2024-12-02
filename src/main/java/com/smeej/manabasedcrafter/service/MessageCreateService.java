package com.smeej.manabasedcrafter.service;

import com.austinv11.servicer.Service;
import com.smeej.manabasedcrafter.listeners.EventListener;
import com.smeej.manabasedcrafter.listeners.MessageListener;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

@Service
public class MessageCreateService extends MessageListener implements EventListener<MessageCreateEvent> {
    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
       return processMessage(event.getMessage());
    }
}
