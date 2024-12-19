package com.smeej.manabasedcrafter.services;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing mana symbols in a card's mana cost by parsing them from Scryfall card data.
 *
 * This service is designed to process mana cost strings retrieved from the Scryfall API responses
 * and extract individual mana symbols (e.g., "{W}", "{U}", "{2}"). The extracted mana symbols
 * are then counted and stored in a provided map for analysis or visualization purposes.
 *
 * Key Features:
 * - Parses mana cost details from a ScryfallResponse object.
 * - Identifies and extracts individual mana symbols using regular expressions.
 * - Counts the occurrences of each mana symbol and updates a provided map with the counts.
 * - Handles null or missing mana cost data gracefully.
 */
@Service
public class ScryfallManaSymbolService {

    private static final Pattern MANA_SYMBOL_PATTERN = Pattern.compile("\\{([^}]*)\\}");

    public void parseManaSymbols(ScryfallResponse response, Map<String, Integer> manaCounts) {
        if (response == null || response.getManaCost() == null) return;

        String manaCost = response.getManaCost();
        Matcher matcher = MANA_SYMBOL_PATTERN.matcher(manaCost);

        while (matcher.find()) {
            String symbol = matcher.group(1); // Extracted symbol (e.g., W, U, 2)
            manaCounts.merge(symbol, 1, Integer::sum);
        }
    }
}
