package com.smeej.manabasedcrafter.utilities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManaSymbolUtils {

    private static final Map<String, String> SYMBOL_TO_NAME = Map.of(
            "U", "Blue",
            "B", "Black",
            "G", "Green",
            "W", "White",
            "R", "Red"
    );

    private static final Map<String, String> SYMBOL_TO_COLOR = Map.of(
            "U", "#1E90FF",   // Blue
            "B", "#000000",       // Black
            "G", "#228B22",       // Green
            "W", "#FFFFFF",       // White
            "R", "#FF4500"            // Red
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
