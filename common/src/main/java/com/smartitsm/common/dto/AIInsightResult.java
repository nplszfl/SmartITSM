package com.smartitsm.common.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * AI insight result for analytics and suggestions.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIInsightResult {
    private String insight;       // The insight message
    private String insightType;   // TREND, ANOMALY, RECOMMENDATION, PREDICTION, WORKFLOW_OPTIMIZATION
    private String category;      // Category of the insight
    private String title;
    private String description;
    private Double confidence;
    private String[] recommendations;
    private List<String> recommendationList;  // Alternative list-based recommendations
    private Map<String, Object> data;
    private List<String> affectedEntities;
    private String severity;      // CRITICAL, WARNING, INFO
}
