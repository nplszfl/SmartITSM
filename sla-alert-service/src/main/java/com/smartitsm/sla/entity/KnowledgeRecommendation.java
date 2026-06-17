package com.smartitsm.sla.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartitsm.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Knowledge article recommendation record for a ticket.
 *
 * Captures relevance score plus user feedback (clicked / helpful) so
 * the recommendation engine can learn over time.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_recommendation")
public class KnowledgeRecommendation extends BaseEntity {

    private Long ticketId;

    private Long knowledgeId;

    // Ranking
    private Double relevanceScore;    // 0..1

    // Snapshot of why we recommended it
    private String matchReason;

    // Feedback
    private LocalDateTime recommendedAt;
    private Boolean clicked;
    private LocalDateTime clickedAt;
    private Boolean helpful;
    private LocalDateTime feedbackAt;
}