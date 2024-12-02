package com.smeej.manabasedcrafter.service;

import com.austinv11.servicer.Service;
import com.smeej.manabasedcrafter.listeners.EventListener;
import com.smeej.manabasedcrafter.listeners.MessageListener;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;

@Service
public class MessageUpdateService extends MessageListener implements EventListener<MessageUpdateEvent> {
    @Override
    public Class<MessageUpdateEvent> getEventType() {
        return MessageUpdateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageUpdateEvent event) {
        return Mono.just(event)
                .filter(MessageUpdateEvent::isContentChanged)
                .flatMap(MessageUpdateEvent::getMessage)
                .flatMap(super::processMessage);
    }
}
