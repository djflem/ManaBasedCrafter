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
import java.util.Set;
import java.util.stream.Collectors;

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
                .onErrorResume(error -> handleError(event, error));
    }

    private Mono<String> extractDeckFileContent(ChatInputInteractionEvent event) {
        return event.getOption("textorcsvfile")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment)
                .map(attachment -> {
                    String fileName = attachment.getFilename();
                    if (!isSupportedFileExtension(fileName)) {
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

    private boolean isSupportedFileExtension(String fileName) {
        return SUPPORTED_FILE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
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

        // Map mana symbols to display-friendly labels and their corresponding colors
        Map<String, String> symbolToName = Map.of(
                "U", "Blue",
                "B", "Black",
                "G", "Green",
                "W", "White",
                "R", "Red"
        );

        Map<String, String> symbolToColor = Map.of(
                "U", "#1E90FF",   // Blue
                "B", "#000000",   // Black
                "G", "#228B22",   // Green
                "W", "#FFFFFF",   // White
                "R", "#FF4500"    // Red
        );

        // Filter and transform manaCounts to only include colors
        Map<String, Integer> chartData = new HashMap<>();
        StringBuilder colors = new StringBuilder();

        manaCounts.forEach((symbol, count) -> {
            if (symbolToName.containsKey(symbol)) {
                String name = symbolToName.get(symbol);
                chartData.put(name, chartData.getOrDefault(name, 0) + count);

                // Append color without quotes
                if (colors.length() > 0) {
                    colors.append(",");
                }
                colors.append(symbolToColor.get(symbol));
            }
        });

        // Generate the pie chart URL with colors
        String chartUrl = quickChartService.generateCustomPieChartUrl(chartData, colors.toString());

        // Prepare the reply message
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
