package com.smartitsm.classification.dto;

import lombok.Data;
import lombok.Builder;

/**
 * Response DTO for ticket classification result.
 */
@Data
@Builder
public class ClassificationResponse {

    private Long ticketId;
    private String ticketNumber;

    // Classification results
    private String category;
    private String subCategory;
    private Double categoryConfidence;
    private String classificationMethod;

    // Priority results
    private String priority;
    private Double priorityScore;
    private String priorityReasoning;

    // Routing results
    private String assignedGroup;
    private String suggestedAssignee;
    private Double routingConfidence;

    // Processing metadata
    private Long processingTimeMs;
    private String status;
    private String message;
}