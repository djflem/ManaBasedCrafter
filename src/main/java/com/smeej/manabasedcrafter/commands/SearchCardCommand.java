package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.scryfallservices.ScryfallCardResponse;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SearchCardCommand implements SlashCommand {

    private final WebClient webClient;

    // Initialize WebClient with Scryfall base URL and required headers
    public SearchCardCommand(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.scryfall.com")
                .defaultHeader("User-Agent", "ManaBasedCrafterBot/1.0 (contact: djfleming.metis@gmail.com)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public String getName() {
        return "searchcard";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Extract the card name from the command input
        String searchedCardName = event.getOption("cardname")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");

        // Query Scryfall for the card
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/cards/named")
                        .queryParam("fuzzy", searchedCardName)
                        .build())
                .retrieve()
                .bodyToMono(ScryfallCardResponse.class) // Map response to a DTO class
                .flatMap(card -> {
                        String imageUrl = card.getImageUris().get("normal"); // Fetch card image URL
                        return event.reply()
                                .withEphemeral(false)
                                .withContent(imageUrl); // Send the image URL as a reply
                })
                .onErrorResume(e -> event.reply()
                        .withEphemeral(true)
                        .withContent("Card not found. Please check the spelling."));
    }
}
