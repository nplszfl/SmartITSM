package com.smartitsm.asset.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for asset response with all details.
 */
@Data
@Builder
public class AssetResponseDTO {
    
    private Long id;
    private String assetNumber;
    private String name;
    private String description;
    
    // Classification
    private String assetType;
    private String subType;
    private String manufacturer;
    private String model;
    private String serialNumber;
    
    // Status
    private String status;
    private String healthStatus;
    private Double healthScore;
    private String aiRecommendations;
    
    // Ownership
    private String location;
    private String department;
    private String assignedTo;
    private String assignedGroup;
    
    // Technical
    private String ipAddress;
    private String macAddress;
    private String hostname;
    private String operatingSystem;
    private Integer cpuCores;
    private Integer memoryGB;
    private Integer storageGB;
    
    // Metrics
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Double networkLatency;
    private Double errorRate;
    private LocalDateTime lastHealthCheck;
    private LocalDateTime lastMonitoring;
    
    // Lifecycle
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String warrantyStatus;
    private LocalDate installationDate;
    private LocalDate retirementDate;
    private Integer expectedLifeYears;
    
    // Financial
    private Double purchaseCost;
    private Double maintenanceCost;
    private Double replacementCost;
    
    // Risk
    private Double failureProbability;
    private String riskLevel;
    private String businessImpact;
    
    // Maintenance
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private Integer maintenanceIntervalDays;
    private Double maintenanceCompliance;
    
    // Metadata
    private String vendor;
    private String supportLevel;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
