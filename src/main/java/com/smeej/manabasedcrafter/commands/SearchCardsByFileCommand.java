package com.smeej.manabasedcrafter.commands;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import com.smeej.manabasedcrafter.services.QuickChartService;
import com.smeej.manabasedcrafter.services.ScryfallManaSymbolService;
import com.smeej.manabasedcrafter.services.ScryfallSearchCardService;
import com.smeej.manabasedcrafter.utilities.ErrorMessages;
import com.smeej.manabasedcrafter.utilities.FileProcessingUtils;
import com.smeej.manabasedcrafter.utilities.ManaSymbolUtils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static final Duration REQUEST_DELAY = Duration.ofMillis(100); // 100 ms delay
    private static final Set<String> SUPPORTED_FILE_EXTENSIONS = Set.of(".txt", ".csv");
    private static final int MAX_UNIQUE_CARDS = 101; // 101 max card limit for Commander (1 extra just because)
    private static final int MAX_KB_FILESIZE = 5000; // 5 kb limit

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
    public Mono<Void> handleCommand(ChatInputInteractionEvent event) {
        return event.deferReply() // Defer reply immediately to avoid interaction timeout
                .then(getDeckFileContent(event)) // Retrieve and validate deck file content
                .flatMap(fileContent -> {
                    Map<String, Integer> deck = FileProcessingUtils.parseDeckFile(fileContent);

                    if (deck == null) { // Null check for invalid deck parsing
                        return event.editReply(ErrorMessages.WRONG_AMOUNT_CARDS).then();
                    }

                    return processCardEntries(Flux.fromIterable(deck.entrySet())) // Process card entries
                            .delayElements(Duration.ofMillis(100)) // Add 100ms delay between API calls
                            .collectList()
                            .flatMap(responses -> generateManaChart(responses, event));
                })
                .onErrorResume(error -> handleError(event, error)); // Handle unexpected errors
    }

    private Mono<String> getDeckFileContent(ChatInputInteractionEvent event) {
        return event.getOption("textorcsvfile")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asAttachment)
                .map(attachment -> {
                    String fileName = attachment.getFilename();

                    // Validate file extension
                    if (!FileProcessingUtils.isSupportedFileExtension(fileName, SUPPORTED_FILE_EXTENSIONS)) {
                        throw new IllegalArgumentException(ErrorMessages.INVALID_FILE_EXTENSION);
                    }

                    // Download and validate file content
                    return downloadFileContent(attachment.getUrl())
                            .flatMap(content -> {
                                if (content.length() > MAX_KB_FILESIZE) { // Check for file size limit
                                    return Mono.error(new IllegalArgumentException(ErrorMessages.FILE_TOO_LARGE));
                                }
                                return Mono.just(content);
                            });
                })
                .orElse(Mono.error(new IllegalArgumentException(ErrorMessages.GENERIC_ERROR))); // Handle missing file case
    }

    private Flux<ScryfallResponse> processCardEntries(Flux<Map.Entry<String, Integer>> cardEntries) {
        return cardEntries
                .delayElements(REQUEST_DELAY) // Enforce 100ms delay between requests
                .flatMap(entry -> {
                    try {
                        String cardName = FileProcessingUtils.validateAndEncodeCardName(entry.getKey());
                        int quantity = entry.getValue();

                        return Flux.range(0, quantity) // Repeat for each card quantity
                                .flatMap(i -> scryfallSearchCardService.searchCardByName(cardName))
                                .onErrorResume(e -> {
                                    LOGGER.warn("Failed to fetch card '{}': {}", entry.getKey(), e.getMessage());
                                    return Mono.empty(); // Skip failed cards
                                });
                    } catch (Exception e) {
                        LOGGER.warn("Invalid card entry '{}': {}", entry.getKey(), e);
                        return Flux.empty(); // Skip invalid entries
                    }
                });
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
                LOGGER.warn("Failed to fetch card data.");
            } else {
                scryfallManaSymbolService.parseManaSymbols(response, manaCounts);
            }
        }

        if (manaCounts.size() > MAX_UNIQUE_CARDS) {
            return event.editReply("Deck contains more than the allowed " + MAX_UNIQUE_CARDS + " unique cards. Please reduce the deck size.").then();
        }

        Map<String, Integer> chartData = ManaSymbolUtils.filterAndTransformManaCounts(manaCounts);
        String colors = ManaSymbolUtils.buildColorString(chartData);

        String chartUrl = quickChartService.generateCustomPieChartUrl(chartData, colors);

        StringBuilder urlLink = new StringBuilder()
                .append("[Mana Symbol Chart](").append(chartUrl).append(")");
        if (failedCards > 0) {
            urlLink.append("\n⚠️ ").append(failedCards).append(" cards could not be processed.");
        }

        return event.editReply(urlLink.toString()).then();
    }

    @Override
    public Mono<Void> handleError(ChatInputInteractionEvent event, Throwable error) {
        LOGGER.error("Error in command [{}]: {}", getName(), error.getMessage(), error);
        return event.reply()
                .withEphemeral(true)
                .withContent(ErrorMessages.GENERIC_ERROR);
    }
}
