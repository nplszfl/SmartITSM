package com.smartitsm.sla.dto;

import lombok.Data;
import lombok.Builder;

/**
 * SLA tier statistics.
 */
@Data
@Builder
public class SlaTierStats {
    private String slaTier;
    private Long total;
    private Long met;
    private Long breached;
    private Double complianceRate;
    private Double avgResponseTimeMinutes;
    private Double avgResolutionTimeMinutes;
}