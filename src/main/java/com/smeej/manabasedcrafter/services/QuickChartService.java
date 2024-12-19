package com.smeej.manabasedcrafter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A service for generating chart URLs using the QuickChart API based on provided data.
 *
 * This service allows the creation of pie chart URLs by taking chart data and associated
 * properties, such as colors, and constructing a corresponding URL. The generated URLs
 * can be used to visualize the provided data as a pie chart hosted on the QuickChart platform.
 *
 * Features:
 * - Builds pie chart JSON structures based on input data.
 * - Encodes the chart details for use with the QuickChart API.
 * - Returns a complete URL for the generated pie chart.
 *
 * Methods:
 * - generateCustomPieChartUrl: Generates a QuickChart API-compatible URL for a pie chart.
 */
@Service
public class QuickChartService {

    public String generateCustomPieChartUrl(Map<String, Integer> chartData, String colors) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Build the background color array properly
            List<String> colorList = Arrays.stream(colors.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            // Build the chart JSON structure
            Map<String, Object> chart = new HashMap<>();
            chart.put("type", "pie");

            Map<String, Object> data = new HashMap<>();
            data.put("labels", chartData.keySet());
            data.put("datasets", List.of(Map.of(
                    "data", chartData.values(),
                    "backgroundColor", colorList
            )));

            chart.put("data", data);

            // Convert to JSON and URL-encode
            String chartJson = objectMapper.writeValueAsString(chart);
            String encodedJson = URLEncoder.encode(chartJson, StandardCharsets.UTF_8);

            // Return the QuickChart URL
            return "https://quickchart.io/chart?c=" + encodedJson;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build chart JSON", e);
        }
    }
}
