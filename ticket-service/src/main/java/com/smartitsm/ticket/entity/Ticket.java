package com.smartitsm.ticket.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Ticket entity - core domain model for ITSM.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ticket")
public class Ticket extends BaseEntity {

    private String ticketNumber;       // Unique ticket reference (e.g., TKT-20240101-0001)
    private String title;
    private String description;

    // Classification
    private String category;            // INCIDENT, SERVICE_REQUEST, CHANGE_REQUEST, PROBLEM
    private String subCategory;
    private String item;                // Service catalog item

    // Status and Priority
    private String status;              // NEW, OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED
    private String priority;            // CRITICAL, HIGH, MEDIUM, LOW
    private String urgency;             // CRITICAL, HIGH, MEDIUM, LOW
    private String impact;              // CRITICAL, HIGH, MEDIUM, LOW

    // Assignment
    private String assignedTo;
    private String assignedGroup;
    private String assignmentReason;

    // Requester
    private String requesterId;
    private String requesterName;
    private String requesterEmail;
    private String requesterDepartment;
    private String requesterPriority;   // VIP, NORMAL

    // SLAs
    private String slaTier;            // P1, P2, P3, P4
    private LocalDateTime firstResponseDue;
    private LocalDateTime resolutionDue;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    // AI/ML fields
    private Double aiScore;            // AI-calculated priority score (0-100)
    private String aiConfidence;       // HIGH, MEDIUM, LOW
    private String aiReasoning;        // AI explanation for score
    private String aiRecommendedAction;
    private Integer predictedResolutionHours;
    private String suggestedAssignee;

    // Asset/CI relation
    private Long assetId;
    private String assetName;

    // Workflow
    private Long workflowInstanceId;
    private String currentWorkflowStep;

    // Metrics
    private Integer affectedUsers;
    private String businessValue;
    private String downtimeImpact;
    private String location;

    // Closure
    private String resolutionCode;     // FIXED, WORKAROUND, DUPLICATE, CANNOT_REPRODUCE, etc.
    private String resolutionNotes;
    private String closureNotes;

    // Metadata
    private String source;              // PORTAL, EMAIL, PHONE, CHAT, API
    private String channel;
    private String externalTicketId;    // For integration with external systems

    // Satisfaction
    private Integer satisfactionRating; // 1-5
    private String satisfactionComment;
}