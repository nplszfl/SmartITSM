package com.smartitsm.analytics.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class AnalyticsSummary {
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private long breachedSLAs;
    private long totalAssets;
    private long healthyAssets;
    private long atRiskAssets;
    private long activeWorkflows;
    private long completedWorkflows;
    private double avgResolutionHours;
    private double avgCSAT;
    private LocalDateTime generatedAt;
}
