package com.smartitsm.asset.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for asset usage statistics.
 */
@Data
@Builder
public class AssetUsageStatsDTO {

    // Usage metrics
    private Long totalAssets;
    private Long activeAssets;
    private Long inactiveAssets;
    private Long retiredAssets;
    
    // Utilization
    private Double overallUtilizationRate;
    
    // By category
    private Long serversCount;
    private Double serversUtilization;
    private Long workstationsCount;
    private Double workstationsUtilization;
    private Long networkDevicesCount;
    private Double networkUtilization;
    private Long storageCount;
    private Double storageUtilization;
    
    // Trends
    private Double utilizationTrend;      // Positive = improving, Negative = declining
    private String trendPeriod;            // DAILY, WEEKLY, MONTHLY
    
    // Period info
    private LocalDateTime reportDate;
    private String periodStart;
    private String periodEnd;
}