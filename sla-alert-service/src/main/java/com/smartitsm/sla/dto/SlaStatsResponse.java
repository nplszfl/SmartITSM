package com.smartitsm.sla.dto;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

/**
 * DTO for SLA statistics.
 */
@Data
@Builder
public class SlaStatsResponse {

    // Overall compliance
    private Double overallComplianceRate;
    private Long totalTickets;
    private Long metSla;
    private Long breachedSla;

    // By SLA type
    private Double firstResponseCompliance;
    private Double resolutionCompliance;

    // By tier
    private Map<String, SlaTierStats> tierStats;

    // Trends
    private Double weeklyTrend;
    private Double monthlyTrend;

    // Period info
    private String periodType;
    private String periodStart;
    private String periodEnd;

    @Data
    @Builder
    public static class SlaTierStats {
        private String slaTier;
        private Long total;
        private Long met;
        private Long breached;
        private Double complianceRate;
        private Double avgResponseTimeMinutes;
        private Double avgResolutionTimeMinutes;
    }
}
