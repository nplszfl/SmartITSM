package com.smartitsm.sla.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for knowledge recommendation request.
 */
@Data
@Builder
public class KnowledgeRecommendRequest {

    private Long ticketId;
    private String ticketCategory;
    private String ticketTitle;
    private String ticketDescription;
    private List<String> keywords;     // extracted from description
    private Integer topK;              // defaults to 5
}