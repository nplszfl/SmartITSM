package com.smartitsm.ticket.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for ticket creation requests.
 */
@Data
@Builder
public class TicketCreateDTO {
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String item;
    private String priority;
    private String urgency;
    private String impact;
    private String requesterId;
    private String requesterName;
    private String requesterEmail;
    private String requesterDepartment;
    private Long assetId;
    private Integer affectedUsers;
    private String source;
    private List<String> attachments;
}