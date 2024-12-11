package com.smeej.manabasedcrafter.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.responses.SearchCardByNameResponse;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class SearchCardByNameCommand implements SlashCommand {

    private final WebClient webClient;

    // Initialize WebClient with Scryfall base URL and required headers
    public SearchCardByNameCommand(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.scryfall.com")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "ManaBasedCrafterBot/1.0 (contact: djfleming.metis@gmail.com)")
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
                .bodyToMono(String.class) // Fetch raw JSON as a String
                .doOnNext(json -> System.out.println("Raw JSON Response: " + json)) // Log raw JSON
                .flatMap(json -> {
                    // Parse JSON into your DTO class
                    ObjectMapper objectMapper = new ObjectMapper();
                    SearchCardByNameResponse card;
                    try {
                        card = objectMapper.readValue(json, SearchCardByNameResponse.class);
                    } catch (Exception e) {
                        System.err.println("Error mapping JSON: " + e.getMessage());
                        return event.reply()
                                .withEphemeral(true)
                                .withContent("Error parsing card data.");
                    }

                    // Handle null or missing image_uris
                    Map<String, String> imageUris = card.getImageUris();
                    if (imageUris != null && imageUris.containsKey("normal")) {
                        String imageUrl = imageUris.get("normal");
                        return event.reply()
                                .withEphemeral(false)
                                .withContent(imageUrl);
                    } else {
                        return event.reply()
                                .withEphemeral(true)
                                .withContent("No image available for this card.");
                    }
                })

                .onErrorResume(e -> {
                    System.err.println("Error: " + e.getMessage());
                    return event.reply()
                            .withEphemeral(true)
                            .withContent("Card not found.");
                });
    }
}
