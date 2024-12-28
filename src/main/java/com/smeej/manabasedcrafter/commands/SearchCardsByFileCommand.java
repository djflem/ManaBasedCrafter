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
 * A command implementation that processes and analyzes Magic: The Gathering deck files
 * uploaded by users and generates a visual representation of mana symbol distribution.
 * <p>
 * This command is part of a Discord bot system where users can upload deck files in `.txt`
 * or `.csv` format. The command then uses the Scryfall API to fetch card data for each card
 * in the deck and generates a pie chart visualizing the distribution of mana symbols.
 * <p>
 * Key Features:
 * - Supports `.txt` and `.csv` file formats for the deck file.
 * - Utilizes the Scryfall API for retrieving card details.
 * - Incorporates delays between API requests to comply with rate limits.
 * - Handles and logs errors gracefully if card data retrieval fails.
 * - Analyzes mana symbols using ScryfallManaSymbolService and generates a pie chart using QuickChartService.
 * <p>
 * Dependencies:
 * - ScryfallSearchCardService: For searching and retrieving card data from Scryfall.
 * - ScryfallManaSymbolService: For parsing and analyzing mana symbols from the retrieved card data.
 * - QuickChartService: For generating a pie chart visualizing mana symbol distribution.
 * - WebClient: For downloading file content from Discord and interacting with external APIs.
 * <p>
 * Command Responsibilities:
 * - Registering the command with the name "analyzedeck".
 * - Extracting deck file content from the uploaded file and validating the file extension.
 * - Parsing the deck file to extract card names and quantities.
 * - Fetching card details from the Scryfall API for each card in the uploaded deck.
 * - Managing mana symbol parsing and generating visual charts based on the analysis.
 * <p>
 * Error Handling:
 * - If the uploaded file is missing or has an unsupported extension, an IllegalArgumentException is thrown.
 * - If any errors occur during file processing, external service calls, or chart generation, error responses
 *   are sent to the user with appropriate messages.
 * <p>
 * Interaction Flow:
 * 1. Users invoke the command by uploading a deck file.
 * 2. The file content is extracted and processed to identify card names and quantities.
 * 3. Card data is fetched from the Scryfall API, and mana symbols are analyzed.
 * 4. A pie chart URL is generated to visualize mana symbol distribution.
 * 5. The chart URL and any additional analysis details are sent as a reply to the user.
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

    @Override
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

        // Prepare response message with a clickable link
        StringBuilder message = new StringBuilder("Analysis complete:\n")
                .append("[Mana Symbol Chart](").append(chartUrl).append(")"); // Embed clickable text link
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
                .withContent("Analysis failed.");
    }
}
