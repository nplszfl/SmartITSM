package com.smartitsm.common.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * AI insight result for analytics and suggestions.
 */
@Data
@Builder
public class AIInsightResult {
    private String insightType; // TREND, ANOMALY, RECOMMENDATION, PREDICTION
    private String title;
    private String description;
    private Double confidence;
    private String[] recommendations;
    private Map<String, Object> data;
    private List<String> affectedEntities;
    private String severity; // CRITICAL, WARNING, INFO
}