package com.smartitsm.knowledge.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Knowledge Article Request DTO.
 */
@Data
@Builder
public class KnowledgeArticleRequest {

    private String title;
    
    private String content;
    
    private String summary;
    
    private String category;
    
    private String subCategory;
    
    private String tags;
    
    private String status;
    
    private String authorId;
    
    private String authorName;
    
    private String relatedTicketIds;
    
    private String relatedAssetTypes;
    
    private LocalDateTime expiresAt;
}