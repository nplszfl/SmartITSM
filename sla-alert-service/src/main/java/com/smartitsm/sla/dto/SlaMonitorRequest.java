package com.smartitsm.sla.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for SLA monitoring request.
 */
@Data
@Builder
public class SlaMonitorRequest {

    private Long ticketId;
    private String ticketNumber;
    private String slaType;           // FIRST_RESPONSE, RESOLUTION
    private String slaTier;           // P1, P2, P3, P4
    private String ticketTitle;
    private String ticketStatus;
    private String ticketPriority;
    private String assignedTo;
    private String assignedGroup;
    private String requesterEmail;
    private Integer affectedUsers;
    private LocalDateTime resolutionDue;
    private LocalDateTime firstResponseDue;
}