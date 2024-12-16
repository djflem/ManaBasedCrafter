package com.smeej.manabasedcrafter.services;

import com.smeej.manabasedcrafter.responses.ScryfallResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParseManaSymbolService {

    private static final Pattern MANA_SYMBOL_PATTERN = Pattern.compile("\\{([^}]*)\\}");

    /**
     * Parses mana symbols from a card's mana cost and aggregates their counts.
     *
     * @param response the ScryfallResponse containing the card's mana cost
     * @param manaCounts the map to store aggregated mana symbol counts
     */
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
