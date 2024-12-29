package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeej.manabasedcrafter.utilities.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating custom pie chart URLs using QuickChart.io.
 * <p>
 * This service provides functionality to create a URL for a custom pie chart visualization
 * by constructing a JSON payload and encoding it for use with the QuickChart API.
 * <p>
 * Core Features:
 * - Dynamically generates a pie chart with outlabels displaying percentages, counts, and labels.
 * - Allows customization of chart colors and appearance.
 * - Computes percentages and totals based on the input data.
 * - Encodes the chart configuration into a URL that can be used to render the chart in a browser or application.
 * <p>
 * Responsibilities:
 * - Prepares the input data by calculating percentages and formatting labels.
 * - Configures the styling and customization of the chart's outlabels.
 * - Encodes the JSON chart configuration appropriately for URL use.
 * - Generates a direct URL for rendering the pie chart through QuickChart.io.
 * <p>
 * Error Handling:
 * - Throws a RuntimeException if serialization or URL encoding fails during chart generation.
 * <p>
 * Example Use Case:
 * This service can be used for visualizing various aggregated data where percentages
 * and totals need to be displayed dynamically, such as mana symbol distributions,
 * demographic breakdowns, or similar datasets.
 * <p>
 * Dependencies:
 * - Jackson's ObjectMapper for JSON serialization.
 * - Java's StandardCharset for encoding URLs.
 * <p>
 * Design Notes:
 * - The service assumes proper formatting of input values (e.g., valid color strings and chart data).
 * - It ensures all visual elements required for the chart are dynamically created based on input data.
 */
@Service
public class QuickChartService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public String generateCustomPieChartUrl(Map<String, Integer> chartData, String colors) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Prepare color list
            List<String> colorList = Arrays.stream(colors.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            // Calculate total symbols
            int totalSymbols = chartData.values().stream().mapToInt(Integer::intValue).sum();

            // Create labels with counts and percentages
            List<String> labels = new ArrayList<>();
            List<String> outlabelsText = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : chartData.entrySet()) {
                int count = entry.getValue();
                double percentage = (count * 100.0) / totalSymbols;

                labels.add(entry.getKey());
                outlabelsText.add(String.format("%s (%.1f%%, %d symbols)", entry.getKey(), percentage, count)); // Custom text
            }

            // Build the chart JSON structure
            Map<String, Object> chart = new HashMap<>();
            chart.put("type", "outlabeledPie"); // Chart type

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("datasets", List.of(Map.of(
                    "data", chartData.values(),
                    "backgroundColor", colorList
            )));

            chart.put("data", data);

            // Configure outlabels to display custom text
            Map<String, Object> options = new HashMap<>();
            options.put("plugins", Map.of(
                    "legend", false,
                    "outlabels", Map.of(
                            "text", outlabelsText,
                            "backgroundColor", "white",
                            "borderColor", "black",
                            "borderRadius", "5",
                            "borderWidth", "1",
                            "color", "black",
                            "stretch", 35,
                            "font", Map.of(
                                    "resizable", true,
                                    "minSize", 12,
                                    "maxSize", 18,
                                    "weight", "bold"
                            )
                    )
            ));

            chart.put("options", options);

            // Convert to JSON and URL-encode
            String chartJson = objectMapper.writeValueAsString(chart);
            String encodedJson = URLEncoder.encode(chartJson, StandardCharsets.UTF_8);

            // Return the QuickChart URL
            return "https://quickchart.io/chart?c=" + encodedJson;

        } catch (Exception e) {
            LOGGER.error("Error generating chart URL: {}", e.getMessage(), e);
            throw new RuntimeException(ErrorMessages.GENERIC_ERROR, e);
        }
    }
}
