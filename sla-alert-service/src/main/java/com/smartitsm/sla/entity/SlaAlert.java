package com.smartitsm.sla.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * SLA alert record entity.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sla_alert")
public class SlaAlert extends BaseEntity {

    private Long ticketId;
    private String ticketNumber;

    // SLA type
    private String slaType;             // FIRST_RESPONSE, RESOLUTION
    private String slaTier;             // P1, P2, P3, P4

    // Timing
    private LocalDateTime slaDueAt;
    private Long remainingSeconds;
    private String remainingTimeDisplay;

    // Alert status
    private String alertLevel;          // GREEN, YELLOW, ORANGE, RED, BREACHED
    private String alertStatus;         // PENDING, SENT, ACKNOWLEDGED, RESOLVED
    private LocalDateTime alertSentAt;
    private String notificationChannel; // EMAIL, SMS, PUSH, SYSTEM

    // Breach tracking
    private Boolean breached;
    private LocalDateTime breachedAt;
    private Long breachDurationSeconds;

    // Notification targets
    private String notifyAssignedTo;
    private String notifyAssignedGroup;
    private String notifyRequester;
    private String additionalRecipients;

    // Alert content
    private String alertTitle;
    private String alertMessage;
    private Integer reminderCount;

    // Ticket details snapshot
    private String ticketTitle;
    private String ticketStatus;
    private String ticketPriority;
    private String assignedTo;
    private String assignedGroup;
}