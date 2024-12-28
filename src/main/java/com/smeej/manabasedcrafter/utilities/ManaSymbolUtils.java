package com.smeej.manabasedcrafter.utilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for handling and transforming mana symbols and their associated
 * metadata such as names and colors. This class provides functionalities to
 * process mana symbol counts and generate corresponding color strings for graphical
 * representation.
 *
 * The class operates on predefined mappings between mana symbols, their names, and
 * associated colors, which are used for visualization purposes.
 */
public class ManaSymbolUtils {

    private static final Map<String, String> SYMBOL_TO_NAME = Map.of(
            "U", "Blue",
            "B", "Black",
            "G", "Green",
            "W", "White",
            "R", "Red"
    );

    private static final Map<String, String> SYMBOL_TO_COLOR = Map.of(
            "U", "#0e68ab",
            "B", "#160b00",
            "G", "#00743f",
            "W", "#f8e7b9",
            "R", "#d31f2a"
    );

    public static Map<String, Integer> filterAndTransformManaCounts(Map<String, Integer> manaCounts) {
        Map<String, Integer> chartData = new LinkedHashMap<>(); // Preserve insertion order
        SYMBOL_TO_NAME.forEach((symbol, name) -> {
            if (manaCounts.containsKey(symbol)) {
                chartData.put(name, manaCounts.get(symbol));
            }
        });
        return chartData;
    }

    public static String buildColorString(Map<String, Integer> chartData) {
        List<String> colors = chartData.keySet().stream()
                .map(label -> {
                    String symbol = SYMBOL_TO_NAME.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(label))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
                    return SYMBOL_TO_COLOR.getOrDefault(symbol, "#000000");
                })
                .collect(Collectors.toList());

        return String.join(",", colors);
    }
}
