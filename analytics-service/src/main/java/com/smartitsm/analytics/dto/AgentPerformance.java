package com.smartitsm.analytics.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AgentPerformance {
    private String agentName;
    private long ticketsAssigned;
    private long ticketsResolved;
    private double resolutionRate;
    private double avgResolutionHours;
    private double avgCSAT;
    private long breachedSLAs;
}
