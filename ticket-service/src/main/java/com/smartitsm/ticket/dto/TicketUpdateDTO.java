package com.smartitsm.ticket.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for full ticket update (PUT) requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUpdateDTO {
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String item;
    private String status;
    private String priority;
    private String urgency;
    private String impact;
    private String assignedTo;
    private String assignedGroup;
    private String assignmentReason;
    private String requesterId;
    private String requesterName;
    private String requesterEmail;
    private String requesterDepartment;
    private String requesterPriority;
    private String slaTier;
    private Long assetId;
    private String assetName;
    private Long workflowInstanceId;
    private String currentWorkflowStep;
    private Integer affectedUsers;
    private String businessValue;
    private String downtimeImpact;
    private String location;
    private String resolutionCode;
    private String resolutionNotes;
    private String closureNotes;
    private String source;
    private String channel;
    private String externalTicketId;
    private Integer satisfactionRating;
    private String satisfactionComment;
}