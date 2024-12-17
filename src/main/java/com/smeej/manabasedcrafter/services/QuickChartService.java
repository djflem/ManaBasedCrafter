package com.smeej.manabasedcrafter.services;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QuickChartService {

    private static final String QUICKCHART_BASE_URL = "https://quickchart.io/chart";

    public String generatePieChartUrl(Map<String, Integer> data) {
        StringBuilder labels = new StringBuilder();
        StringBuilder values = new StringBuilder();

        data.forEach((label, value) -> {
            if (labels.length() > 0) {
                labels.append(",");
                values.append(",");
            }
            labels.append("\"").append(label).append("\"");
            values.append(value);
        });

        // Build the QuickChart URL
        return QUICKCHART_BASE_URL + "?c={type:'pie',data:{labels:["
                + labels + "],datasets:[{data:[" + values + "]}]}}";
    }
}
