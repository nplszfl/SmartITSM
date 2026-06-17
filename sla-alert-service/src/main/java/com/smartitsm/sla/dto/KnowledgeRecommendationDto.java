package com.smartitsm.sla.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for knowledge article recommendation.
 */
@Data
@Builder
public class KnowledgeRecommendationDto {

    private Long id;
    private Long ticketId;
    private Long knowledgeId;
    private String knowledgeTitle;
    private String knowledgeSummary;

    private Double relevanceScore;
    private String matchReason;

    private Boolean clicked;
    private LocalDateTime clickedAt;
    private Boolean helpful;
    private LocalDateTime feedbackAt;

    private LocalDateTime recommendedAt;
    private LocalDateTime createdAt;
}