package com.smartitsm.sla.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for SLA breach risk prediction.
 */
@Data
@Builder
public class SlaBreachRiskDto {

    private Long id;
    private Long ticketId;
    private Long slaPolicyId;

    private Double currentProgress;
    private Double expectedProgress;
    private Double progressGap;

    private Double riskScore;
    private String riskLevel;             // LOW, MEDIUM, HIGH, CRITICAL

    private LocalDateTime predictedBreachAt;
    private LocalDateTime slaDueAt;

    private String status;
    private String ticketCategory;
    private String ticketPriority;

    // Factor breakdown for explainability
    private Double progressGapFactor;
    private Double timeElapsedRatioFactor;
    private Double historicalBreachRateFactor;
    private Double priorityFactor;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}