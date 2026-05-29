package com.smartitsm.classification.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Ticket classification record entity.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ticket_classification")
public class TicketClassification extends BaseEntity {

    private Long ticketId;
    private String ticketNumber;

    // Classification results
    private String category;           // INCIDENT, SERVICE_REQUEST, CHANGE_REQUEST, PROBLEM
    private String subCategory;
    private Double categoryConfidence;
    private String classificationMethod; // KEYWORD, AI, HYBRID

    // Priority detection
    private String priority;            // CRITICAL, HIGH, MEDIUM, LOW
    private Double priorityScore;        // 0-100
    private String priorityReasoning;

    // Routing assignment
    private String assignedGroup;
    private String suggestedAssignee;
    private String routingReasoning;
    private Double routingConfidence;

    // AI processing
    private String aiModel;
    private String aiPrompt;
    private String aiResponse;
    private LocalDateTime aiProcessedAt;

    // Keywords matched
    private String matchedKeywords;
    private String extractedEntities;   // JSON format for extracted entities
}