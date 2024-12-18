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
