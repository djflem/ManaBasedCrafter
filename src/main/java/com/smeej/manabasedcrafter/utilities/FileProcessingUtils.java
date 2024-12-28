package com.smeej.manabasedcrafter.utilities;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class providing methods for processing file contents related to card deck management.
 * This class includes functionality for parsing deck files and validating file extensions.
 * <p>
 * Typical use cases include:
 * - Reading and parsing deck file contents into a map of card names and their quantities.
 * - Validating whether a given file extension is supported.
 */
public class FileProcessingUtils {

    public static Map<String, Integer> parseDeckFile(String fileContent) {
        Map<String, Integer> deck = new LinkedHashMap<>(); // Preserve order for clarity

        fileContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty()) // Ignore empty lines
                .forEach(line -> {
                    String[] parts = line.split(" ", 2); // Split into [quantity, card name]
                    int quantity = 1; // Default quantity

                    String cardName;
                    if (parts.length > 1 && parts[0].matches("\\d+")) {
                        quantity = Integer.parseInt(parts[0]); // Parse quantity
                        cardName = parts[1]; // Card name is the rest
                    } else {
                        cardName = line; // Entire line is the card name
                    }

                    // Merge quantity if card name already exists
                    deck.merge(cardName, quantity, Integer::sum);
                });

        return deck;
    }

    public static boolean isSupportedFileExtension(String fileName, Set<String> supportedExtensions) {
        return supportedExtensions.stream().anyMatch(fileName::endsWith);
    }
}
