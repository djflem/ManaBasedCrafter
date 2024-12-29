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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command implementation for analyzing deck files in .txt or .csv format, specifically for analyzing
 * Magic: The Gathering cards using data fetched from the Scryfall API. This class processes a user-provided
 * deck file, validates its contents, and generates a visual mana curve chart representing the distribution
 * of mana symbols in the analyzed deck.
 * <p>
 * Key Features:
 * - Validates user-uploaded deck files for supported formats and size constraints.
 * - Retrieves card information from the Scryfall API, including mana symbols.
 * - Generates a mana distribution chart using the QuickChart API.
 * - Provides appropriate feedback to users in case of errors or invalid inputs.
 * <p>
 * Supported File Extensions:
 * - .txt
 * - .csv
 * <p>
 * Key Constants:
 * - REQUEST_DELAY (100ms): Delay between consecutive Scryfall API requests to avoid overloading.
 * - SUPPORTED_FILE_EXTENSIONS: Set of allowed file extensions for user-uploaded deck files.
 * - MAX_UNIQUE_CARDS (101): Maximum allowed number of unique cards in a deck.
 * - MAX_KB_FILESIZE (5000 KB): Maximum allowed deck file size.
 * - DEFAULT_ERROR_MESSAGE: Default error message returned to the user for generic issues.
 * <p>
 * Dependencies:
 * - ScryfallSearchCardService: Retrieves card details from the Scryfall API.
 * - ScryfallManaSymbolService: Parses and processes mana symbols from card data.
 * - QuickChartService: Generates visual charts based on processed mana data.
 * - WebClient: HTTP client for downloading deck files from user-provided URLs.
 * <p>
 * Methods:
 * - getName: Returns the name identifier of the slash command ("analyzedeck").
 * - handle: Primary handler for processing the command input and generating output.
 * - processCardEntries: Processes card name and quantity data, fetching and validating through Scryfall API.
 * - validateAndEncodeCardName: Validates and encodes card names to ensure compatibility with API calls.
 * - getDeckFileContent: Retrieves and validates the content of the uploaded file.
 * - downloadFileContent: Downloads file content from a provided URL using WebClient.
 * - generateManaChart: Creates and returns a mana curve chart based on card data.
 * - handleError: Manages and logs errors, providing user-friendly messages when exceptions occur.
 */
@Component
public class SearchCardsByFileCommand implements SlashCommand {

    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // 100 ms delay
    private static final Set<String> SUPPORTED_FILE_EXTENSIONS = Set.of(".txt", ".csv");
    private static final int MAX_UNIQUE_CARDS = 101; // 101 max card limit for Commander (1 extra just because)
    private static final int MAX_KB_FILESIZE = 5000; // 5 kb limit
    private static final String DEFAULT_ERROR_MESSAGE = "An error occurred while processing your analysis. Please check the deck contents and try again.";

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

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply()
                .then(getDeckFileContent(event))
                .flatMapMany(fileContent -> Flux.fromIterable(FileProcessingUtils.parseDeckFile(fileContent).entrySet()))
                .transform(this::processCardEntries) // Use dedicated processing function
                .delayElements(REQUEST_DELAY)
                .onErrorResume(e -> Mono.empty())
                .collectList()
                .flatMap(responses -> generateManaChart(responses, event))
                .onErrorResume(error -> handleError(event, error));
    }

    private Flux<ScryfallResponse> processCardEntries(Flux<Map.Entry<String, Integer>> cardEntries) {
        return cardEntries.flatMap(entry -> {
            String cardName = validateAndEncodeCardName(entry.getKey());
            int quantity = entry.getValue();
            return Flux.range(0, quantity)
                    .flatMap(i -> scryfallSearchCardService.searchCardByName(cardName));
        });
    }

    private String validateAndEncodeCardName(String cardName) {
        if (cardName == null || cardName.trim().isEmpty()) {
            throw new IllegalArgumentException("Card name cannot be empty.");
        }

        // Normalize and encode the card name
        return URLEncoder.encode(cardName.trim(), StandardCharsets.UTF_8);
    }

    private Mono<String> getDeckFileContent(ChatInputInteractionEvent event) {
        return event.getOption("textorcsvfile")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment)
                .map(attachment -> {
                    String fileName = attachment.getFilename();
                    if (!FileProcessingUtils.isSupportedFileExtension(fileName, SUPPORTED_FILE_EXTENSIONS)) {
                        throw new IllegalArgumentException("Supported extensions are .txt or .csv.");
                    }
                    return downloadFileContent(attachment.getUrl())
                            .flatMap(content -> {
                                if (content.length() > MAX_KB_FILESIZE) { // 5 KB limit
                                    return Mono.error(new IllegalArgumentException("File size exceeds the 5 KB limit."));
                                }
                                return Mono.just(content);
                            });
                })
                .orElse(Mono.error(new IllegalArgumentException(DEFAULT_ERROR_MESSAGE)));
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

        // Enforce a limit on total unique cards
        if (manaCounts.size() > MAX_UNIQUE_CARDS) {
            return event.editReply("Deck contains more than the allowed " + MAX_UNIQUE_CARDS + " unique cards. Please reduce the deck size.").then();
        }

        // Use ManaSymbolUtils for transformation and color preparation
        Map<String, Integer> chartData = ManaSymbolUtils.filterAndTransformManaCounts(manaCounts);
        String colors = ManaSymbolUtils.buildColorString(chartData);

        // Generate pie chart URL
        String chartUrl = quickChartService.generateCustomPieChartUrl(chartData, colors);

        // Prepare response message with a clickable link
        StringBuilder urlLink = new StringBuilder()
                .append("[Mana Symbol Chart](").append(chartUrl).append(")"); // Embed clickable text link
        if (failedCards > 0) {
            urlLink.append("\n⚠️ ").append(failedCards).append(" cards could not be processed.");
        }

        return event.editReply(urlLink.toString()).then();
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        System.err.println("Error during analyzedeck command: " + error.getMessage());
        error.printStackTrace(); // Log the full stack trace for debugging

        return event.reply()
                .withEphemeral(true)
                .withContent(DEFAULT_ERROR_MESSAGE);
    }
}
