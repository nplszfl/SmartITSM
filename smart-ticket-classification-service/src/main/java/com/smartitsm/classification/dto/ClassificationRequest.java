package com.smartitsm.classification.dto;

import lombok.Data;
import lombok.Builder;

/**
 * Request DTO for ticket classification.
 */
@Data
@Builder
public class ClassificationRequest {

    private Long ticketId;
    private String ticketNumber;
    private String title;
    private String description;
    private String requesterId;
    private String requesterDepartment;
    private String requesterPriority;
    private Integer affectedUsers;
    private String source;

    // Optional context
    private String existingCategory;
    private String existingPriority;
}