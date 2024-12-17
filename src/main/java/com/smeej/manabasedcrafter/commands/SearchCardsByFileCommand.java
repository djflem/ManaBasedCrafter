package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.QuickChartService;
import com.smeej.manabasedcrafter.services.ScryfallManaSymbolService;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchCardsByFileCommand implements SlashCommand {

    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // Extracted constant for the delay duration

    private final ScryfallSearchCardService scryfallSearchCardService;
    private final ScryfallManaSymbolService scryfallManaSymbolService;
    private final QuickChartService quickChartService;
    private final WebClient generalWebClient;

    public SearchCardsByFileCommand(ScryfallSearchCardService scryfallSearchCardService,
                                    ScryfallManaSymbolService scryfallManaSymbolService,
                                    QuickChartService quickChartService,
                                    @Qualifier("generalWebClient") WebClient generalWebClient) {
        this.scryfallSearchCardService = scryfallSearchCardService;
        this.scryfallManaSymbolService = scryfallManaSymbolService;
        this.quickChartService = quickChartService;
        this.generalWebClient = generalWebClient;    }

    @Override
    public String getName() {
        return "analyzedeck";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply()
                .then(extractDeckFileContent(event))
                .onErrorResume(e -> event.editReply("Failed to process the uploaded file. Error: " + e.getMessage()).then(Mono.empty()))
                .map(this::parseCardNames)
                .flatMapMany(Flux::fromIterable)
                .delayElements(REQUEST_DELAY) // Using extracted constant
                .flatMap(cardName -> scryfallSearchCardService.searchCardByName(cardName), 5)
                .onErrorResume(e -> Mono.empty()) // Skip failed API calls
                .collectList()
                .flatMap(responses -> generateManaChart(responses, event))
                .onErrorResume(e -> handleError(event, "An unexpected error occurred: " + e.getMessage())); // Reusable error handler
    }

    private Mono<String> extractDeckFileContent(ChatInputInteractionEvent event) {
        var attachment = event.getOption("textorcsvfile")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment);

        if (attachment.isEmpty()) {
            return Mono.error(new IllegalArgumentException("No file uploaded for processing."));
        }

        String attachmentFileName = attachment.get().getFilename(); // Renamed for clarity
        if (!(attachmentFileName.endsWith(".txt") || attachmentFileName.endsWith(".csv"))) {
            return Mono.error(new IllegalArgumentException("Only .txt or .csv files are supported."));
        }

        return fetchFileContent(attachment.get().getUrl());
    }

    private Mono<String> fetchFileContent(String url) { // Renamed for clarity
        return generalWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }

    private List<String> parseCardNames(String fileContent) {
        return fileContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty()) // Ignore empty lines
                .collect(Collectors.toList());
    }

    private Mono<Void> generateManaChart(List<ScryfallResponse> responses, ChatInputInteractionEvent event) {
        Map<String, Integer> manaCounts = new HashMap<>();
        int failedCards = 0;

        for (ScryfallResponse response : responses) {
            if (response == null) {
                failedCards++;
            } else {
                scryfallManaSymbolService.parseManaSymbols(response, manaCounts);
            }
        }

        String chartUrl = quickChartService.generatePieChartUrl(manaCounts);
        StringBuilder message = new StringBuilder("Here's the mana breakdown for your deck:\n").append(chartUrl);
        if (failedCards > 0) {
            message.append("\n⚠️ ").append(failedCards).append(" cards could not be processed.");
        }
        return event.editReply(message.toString()).then();
    }

    private Mono<Void> handleError(ChatInputInteractionEvent event, String errorMessage) { // Extracted error handling
        return event.editReply(errorMessage).then(Mono.empty());
    }
}
