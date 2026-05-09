package com.smartitsm.common.dto;

import lombok.Data;
import lombok.Builder;

/**
 * AI scoring result for tickets, assets, etc.
 */
@Data
@Builder
public class AIScoreResult {
    private Double score;
    private String reasoning;
    private String confidence; // HIGH, MEDIUM, LOW
    private String[] factors;
    private String recommendedAction;
    private String category;
    private Double priorityScore;
}