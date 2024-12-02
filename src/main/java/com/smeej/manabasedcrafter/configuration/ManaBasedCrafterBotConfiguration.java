package com.smeej.manabasedcrafter.configuration;

import com.smeej.manabasedcrafter.listeners.EventListener;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import discord4j.core.event.domain.Event;
import java.util.List;

@Configuration
public class ManaBasedCrafterBotConfiguration {
    @Value("${token}")
    private String token;

    @Bean
    public <T extends Event>GatewayDiscordClient gatewayDiscordClient(final List<EventListener<T>> eventListeners) {
        final GatewayDiscordClient client = DiscordClientBuilder.create(token)
                .build()
                .login()
                .block();

        for(final EventListener<T> listener : eventListeners) {
            client.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }
        return client;
    }
}
