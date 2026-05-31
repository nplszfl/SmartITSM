package com.smartitsm.knowledge.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Knowledge Search Result DTO - for search results with similarity score.
 */
@Data
@Builder
public class KnowledgeSearchResult {

    private Long articleId;
    
    private String title;
    
    private String summary;
    
    private String category;
    
    private String tags;
    
    private Integer viewCount;
    
    private Integer helpfulCount;
    
    private LocalDateTime publishedAt;
    
    private Double similarityScore;
}