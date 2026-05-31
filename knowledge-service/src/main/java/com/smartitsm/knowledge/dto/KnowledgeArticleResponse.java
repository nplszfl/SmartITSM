package com.smartitsm.knowledge.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Knowledge Article Response DTO.
 */
@Data
@Builder
public class KnowledgeArticleResponse {

    private Long id;
    
    private String articleNumber;
    
    private String title;
    
    private String content;
    
    private String summary;
    
    private String category;
    
    private String subCategory;
    
    private String tags;
    
    private String status;
    
    private String authorId;
    
    private String authorName;
    
    private Integer viewCount;
    
    private Integer helpfulCount;
    
    private Integer notHelpfulCount;
    
    private String approvedBy;
    
    private LocalDateTime approvedAt;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime expiresAt;
    
    private String source;
    
    private String relatedTicketIds;
    
    private String relatedAssetTypes;
    
    private Integer resolutionRate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String createdBy;
    
    private String updatedBy;
}