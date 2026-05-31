package com.smartitsm.knowledge.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Knowledge Article entity - represents a knowledge base article.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_article")
public class KnowledgeArticle extends BaseEntity {

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
}