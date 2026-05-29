package com.smartitsm.classification.dto;

import lombok.Data;
import lombok.Builder;

/**
 * DTO for classification statistics.
 */
@Data
@Builder
public class ClassificationStats {

    private Long totalClassified;
    private Long autoClassified;
    private Long aiClassified;
    private Long keywordClassified;

    // Category breakdown
    private Long incidentCount;
    private Long serviceRequestCount;
    private Long changeRequestCount;
    private Long problemCount;

    // Priority breakdown
    private Long criticalCount;
    private Long highCount;
    private Long mediumCount;
    private Long lowCount;

    // Accuracy metrics
    private Double avgConfidence;
    private Double autoClassificationRate;
}