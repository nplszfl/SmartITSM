package com.smartitsm.asset.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Asset entity - represents IT infrastructure assets managed by ITSM.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_asset")
public class Asset extends BaseEntity {

    private String assetNumber;           // Unique asset identifier (e.g., AST-2024-00001)
    private String name;                  // Asset name
    private String description;           // Detailed description
    
    // Classification
    private String assetType;             // SERVER, NETWORK, STORAGE, ENDPOINT, SOFTWARE, CLOUD, DATABASE
    private String subType;               // Specific type within category
    private String manufacturer;          // OEM/manufacturer
    private String model;                  // Model number/name
    private String serialNumber;           // Hardware serial number
    
    // Status & Health
    private String status;                // ACTIVE, INACTIVE, MAINTENANCE, RETIRED, DISPOSED
    private String healthStatus;          // HEALTHY, WARNING, CRITICAL, UNKNOWN
    private Double healthScore;           // AI-calculated health score (0-100)
    private String aiRecommendations;      // AI maintenance recommendations
    
    // Location & Ownership
    private String location;              // Physical or logical location
    private String department;            // Owning department
    private String assignedTo;             // Assigned user/owner
    private String assignedGroup;         // Responsible team
    
    // Technical Details
    private String ipAddress;              // IP address if applicable
    private String macAddress;            // MAC address
    private String hostname;               // Network hostname
    private String operatingSystem;        // OS version
    private Integer cpuCores;              // CPU core count
    private Integer memoryGB;              // Memory in GB
    private Integer storageGB;             // Storage capacity in GB
    
    // Metrics (updated by monitoring)
    private Double cpuUsage;               // Current CPU usage %
    private Double memoryUsage;             // Current memory usage %
    private Double diskUsage;               // Current disk usage %
    private Double networkLatency;          // Network latency in ms
    private Double errorRate;              // Error rate %
    private LocalDateTime lastHealthCheck; // Last health check timestamp
    private LocalDateTime lastMonitoring;  // Last monitoring data update
    
    // Lifecycle
    private LocalDate purchaseDate;        // Purchase date
    private LocalDate warrantyExpiry;       // Warranty expiration
    private String warrantyStatus;         // ACTIVE, EXPIRED, EXTENDED
    private LocalDate installationDate;     // Installation date
    private LocalDate retirementDate;       // Planned retirement date
    private Integer expectedLifeYears;      // Expected useful life in years
    
    // Financial
    private Double purchaseCost;           // Purchase cost
    private Double maintenanceCost;        // Annual maintenance cost
    private Double replacementCost;        // Estimated replacement cost
    
    // Relationships
    private Long parentAssetId;            // Parent asset in hierarchy
    private String relatedTicketIds;       // Comma-separated ticket IDs
    private String configurationItems;      // Related CIs as JSON
    
    // Risk Assessment
    private Double failureProbability;      // AI-calculated failure probability (0-100)
    private String riskLevel;              // CRITICAL, HIGH, MEDIUM, LOW
    private String businessImpact;         // Impact if asset fails
    
    // Maintenance
    private LocalDate lastMaintenanceDate;  // Last maintenance performed
    private LocalDate nextMaintenanceDate;  // Next scheduled maintenance
    private Integer maintenanceIntervalDays; // Days between maintenance
    private Double maintenanceCompliance;    // Maintenance compliance %
    
    // Metadata
    private String vendor;                 // Vendor/supplier
    private String vendorContact;          // Vendor contact info
    private String supportLevel;           // BASIC, STANDARD, PREMIUM, PREMIUM_PLUS
    private String tags;                   // Comma-separated tags for categorization
}
