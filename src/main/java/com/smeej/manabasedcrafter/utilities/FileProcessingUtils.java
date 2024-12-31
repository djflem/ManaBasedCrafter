package com.smeej.manabasedcrafter.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class providing static factories for processing file contents related to card deck management.
 * This class includes functionality for parsing deck files and validating file extensions.
 * <p>
 * Typical use cases include:
 * - Reading and parsing deck file contents into a map of card names and their quantities.
 * - Validating whether a given file extension is supported.
 */
public class FileProcessingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileProcessingUtils.class);

    public static Map<String, Integer> parseDeckFile(String fileContent) {
        Map<String, Integer> deck = new LinkedHashMap<>(); // Preserve order for clarity
        AtomicInteger totalCards = new AtomicInteger(0); // Track total cards dynamically

        fileContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty()) // Ignore empty lines
                .forEach(line -> {
                    try {
                        int quantity = 1; // Default quantity
                        String cardName;

                        // Check if the line starts with a number
                        if (Character.isDigit(line.charAt(0))) {
                            int spaceIndex = line.indexOf(' ');
                            if (spaceIndex > 0) {
                                quantity = Integer.parseInt(line.substring(0, spaceIndex).trim());
                                cardName = line.substring(spaceIndex + 1).trim(); // Remaining part is the card name
                            } else {
                                LOGGER.warn("Invalid line format, no card name after quantity: {}", line);
                                return; // Skip the line if no card name is found
                            }
                        } else {
                            cardName = line.trim(); // Treat the entire line as the card name
                        }

                        // Merge quantity if the card name already exists
                        deck.merge(cardName.toLowerCase(), quantity, Integer::sum);

                        // Update the total card count
                        totalCards.addAndGet(quantity);

                        // Log the current card being processed and total cards so far
                        LOGGER.info("Processed card: '{}' with quantity: {}. Total cards so far: {}",
                                cardName, quantity, totalCards.get());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse line: {}", line, e);
                    }
                });

        LOGGER.info("Finished parsing deck. Total cards: {}", totalCards.get());

        // Validate total card count
        if (totalCards.get() != 60 && totalCards.get() != 100) {
            LOGGER.error(ErrorMessages.WRONG_AMOUNT_CARDS);
            return null; // Return null if deck size is invalid
        } else {
            return deck; // Return the deck if valid
        }
    }

    public static String validateAndEncodeCardName(String cardName) {
        if (cardName == null || cardName.trim().isEmpty()) {
            throw new IllegalArgumentException("Card name cannot be empty.");
        }

        // Remove apostrophes and sanitize the card name
        String sanitizedCardName = cardName.replace("'", "").replaceAll("[^a-zA-Z0-9\\-\\s,]", "");

        if (sanitizedCardName.isEmpty()) {
            throw new IllegalArgumentException("Card name contains invalid characters only.");
        }

        // Normalize and encode the sanitized card name
        return URLEncoder.encode(sanitizedCardName.trim(), StandardCharsets.UTF_8);
    }

    public static boolean isSupportedFileExtension(String fileName, Set<String> supportedExtensions) {
        return supportedExtensions.stream().anyMatch(fileName::endsWith);
    }
}
