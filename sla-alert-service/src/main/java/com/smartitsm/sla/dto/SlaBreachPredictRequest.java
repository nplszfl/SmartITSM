package com.smartitsm.sla.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for breach risk prediction request.
 */
@Data
@Builder
public class SlaBreachPredictRequest {

    private Long ticketId;
    private Long slaPolicyId;
    private String ticketCategory;     // INCIDENT, SERVICE_REQUEST, ...
    private String ticketPriority;     // HIGH, MEDIUM, LOW
    private Double currentProgress;    // 0..100
    private Double expectedProgress;   // 0..100
    private LocalDateTime slaStartedAt;
    private LocalDateTime slaDueAt;
    private LocalDateTime now;         // inject for testability (defaults to now)
}