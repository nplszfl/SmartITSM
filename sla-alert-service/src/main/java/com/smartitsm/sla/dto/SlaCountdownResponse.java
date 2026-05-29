package com.smartitsm.sla.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for SLA countdown response.
 */
@Data
@Builder
public class SlaCountdownResponse {

    private Long ticketId;
    private String ticketNumber;
    private String slaType;
    private String slaTier;

    // Countdown
    private Long remainingSeconds;
    private String remainingTimeDisplay;
    private Double progressPercentage;

    // Status
    private String alertLevel;
    private String status;
    private LocalDateTime slaDueAt;

    // Ticket info
    private String ticketTitle;
    private String ticketStatus;
    private String assignedTo;
    private String assignedGroup;
}