package com.smartitsm.asset.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for asset health analysis result from AI.
 */
@Data
@Builder
public class AssetHealthDTO {
    
    private Long assetId;
    private String assetName;
    
    // Health Analysis
    private Double healthScore;           // 0-100
    private String riskLevel;             // CRITICAL, HIGH, MEDIUM, LOW
    private String healthStatus;          // HEALTHY, WARNING, CRITICAL, UNKNOWN
    
    // AI Analysis
    private String prediction;            // Failure timeline prediction
    private Double failureProbability;    // 0-100
    private List<String> recommendations;  // Maintenance recommendations
    private LocalDate recommendedMaintenanceWindow;
    private Double estimatedReplacementCost;
    
    // Key Risk Factors
    private List<String> riskFactors;
    
    // Metadata
    private String analyzedAt;
    private String confidence;            // HIGH, MEDIUM, LOW
}
