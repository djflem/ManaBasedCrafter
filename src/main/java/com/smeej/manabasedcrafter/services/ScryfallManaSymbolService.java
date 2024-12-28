package com.smeej.manabasedcrafter.services;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for parsing and analyzing mana symbols from the mana cost field
 * of a ScryfallResponse object. This service handles the extraction and aggregation
 * of mana symbols, providing a processed count of each symbol found.
 * <p>
 * Functionality:
 * - Extracts mana symbols from the "manaCost" field of a ScryfallResponse object using
 *   a predefined pattern ({<symbol>} format).
 * - Populates a given map with counts for each extracted symbol, incrementing the count
 *   for symbols already present in the map.
 * <p>
 * Dependencies:
 * - Utilizes Java's Pattern and Matcher classes for efficient regex-based symbol extraction.
 * - ScryfallResponse: The data structure containing card information, including mana cost.
 * <p>
 * Typical usage involves using this service in conjunction with other services (e.g.,
 * ScryfallSearchCardService) to process a collection of cards' mana costs for analysis
 * or visualization purposes.
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
