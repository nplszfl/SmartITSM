package com.smartitsm.asset.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for updating an existing asset.
 */
@Data
public class AssetUpdateDTO {
    
    private String name;
    private String description;
    private String assetType;
    private String subType;
    private String manufacturer;
    private String model;
    private String serialNumber;
    
    // Status
    private String status;
    private String healthStatus;
    
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
    
    // Monitoring metrics
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Double networkLatency;
    private Double errorRate;
    
    // Lifecycle
    private LocalDate warrantyExpiry;
    private LocalDate retirementDate;
    
    // Financial
    private Double maintenanceCost;
    
    // Maintenance
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private Integer maintenanceIntervalDays;
    
    // Relationships
    private Long parentAssetId;
    private String relatedTicketIds;
    private String configurationItems;
    
    // Metadata
    private String vendor;
    private String vendorContact;
    private String supportLevel;
    private String tags;
}
