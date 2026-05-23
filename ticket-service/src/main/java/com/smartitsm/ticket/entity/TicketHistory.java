package com.smartitsm.ticket.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * TicketHistory entity - audit trail of all changes made to a ticket.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_history")
public class TicketHistory extends BaseEntity {

    private Long ticketId;              // Reference to the ticket
    private String ticketNumber;        // Snapshot of ticket number for history query
    private String fieldName;           // Name of the field that changed
    private String oldValue;            // Previous value
    private String newValue;            // New value
    private String changeType;          // CREATE, UPDATE, STATUS_CHANGE, ASSIGNMENT, COMMENT, SLA, WORKFLOW
    private String changedBy;          // User who made the change
    private String changedByName;       // Display name of user
    private String changeReason;        // Optional reason for the change
    private String ipAddress;           // Client IP address
    private String userAgent;           // Client user agent
}