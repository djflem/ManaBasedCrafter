package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.QuickChartService;
import com.smeej.manabasedcrafter.services.ScryfallManaSymbolService;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import com.smeej.manabasedcrafter.utilities.FileProcessingUtils;
import com.smeej.manabasedcrafter.utilities.ManaSymbolUtils;
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
import java.util.Set;

/**
 * Command implementation for analyzing a deck by processing an uploaded file containing card names.
 * The command retrieves mana information for the cards listed in the file and generates a pie chart
 * showcasing the breakdown of mana symbols used in the deck. Supports files with ".txt" or ".csv"
 * extensions.
 *
 * This command provides the following features:
 * - Extracts and processes a file uploaded via a slash command interaction.
 * - Validates supported file types and retrieves file contents using a WebClient.
 * - Parses card names from the uploaded file content and queries card information via the Scryfall API.
 * - Aggregates mana symbol data using the ScryfallManaSymbolService.
 * - Generates a visual mana breakdown chart using the QuickChartService.
 *
 * The command also includes error handling for invalid files, failed file processing, Scryfall API errors,
 * and other runtime exceptions during processing. Any errors encountered are reported back to the user
 * through the chat interaction response.
 */
@Component
public class SearchCardsByFileCommand implements SlashCommand {

    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // Extracted constant for the delay duration
    private static final Set<String> SUPPORTED_FILE_EXTENSIONS = Set.of(".txt", ".csv");

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
        this.generalWebClient = generalWebClient;
    }

    @Override
    public String getName() {
        return "analyzedeck";
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply()
                .then(extractDeckFileContent(event))
                .flatMapMany(fileContent -> Flux.fromIterable(FileProcessingUtils.parseDeckFile(fileContent).entrySet()))
                .flatMap(entry -> {
                    String cardName = entry.getKey();
                    int quantity = entry.getValue();
                    return Flux.range(0, quantity) // Repeat for the quantity of the card
                            .flatMap(i -> scryfallSearchCardService.searchCardByName(cardName)); // Use cardName correctly
                })
                .delayElements(REQUEST_DELAY) // Delay requests to avoid overloading
                .onErrorResume(e -> Mono.empty()) // Skip failed API calls gracefully
                .collectList()
                .flatMap(responses -> generateManaChart(responses, event)) // Generate the mana chart
                .onErrorResume(error -> handleError(event, error)); // Handle errors gracefully
    }

    private Mono<String> extractDeckFileContent(ChatInputInteractionEvent event) {
        return event.getOption("textorcsvfile")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment)
                .map(attachment -> {
                    String fileName = attachment.getFilename();
                    if (!FileProcessingUtils.isSupportedFileExtension(fileName, SUPPORTED_FILE_EXTENSIONS)) {
                        throw new IllegalArgumentException("Supported extensions are .txt or .csv.");
                    }
                    return downloadFileContent(attachment.getUrl());
                })
                .orElse(Mono.error(new IllegalArgumentException("No file uploaded for processing.")));
    }

    private Mono<String> downloadFileContent(String url) { // Renamed for clarity
        return generalWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
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

        // Use ManaSymbolUtils for transformation and color preparation
        Map<String, Integer> chartData = ManaSymbolUtils.filterAndTransformManaCounts(manaCounts);
        String colors = ManaSymbolUtils.buildColorString(chartData);

        // Generate pie chart URL
        String chartUrl = quickChartService.generateCustomPieChartUrl(chartData, colors);

        // Prepare response message
        StringBuilder message = new StringBuilder("Analysis complete:\n").append(chartUrl);
        if (failedCards > 0) {
            message.append("\n⚠️ ").append(failedCards).append(" cards could not be processed.");
        }

        return event.editReply(message.toString()).then();
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error: " + error.getMessage());
        return event.reply()
                .withEphemeral(true)
                .withContent("Analysis unsuccessful.");
    }
}
