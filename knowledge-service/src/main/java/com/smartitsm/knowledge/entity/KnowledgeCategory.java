package com.smartitsm.knowledge.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Knowledge Category entity - represents a category for organizing knowledge articles.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_category")
public class KnowledgeCategory extends BaseEntity {

    private String name;
    
    private String description;
    
    private Long parentId;
    
    private Integer sortOrder;
    
    private Integer articleCount;
    
    private String icon;
    
    private String color;
}