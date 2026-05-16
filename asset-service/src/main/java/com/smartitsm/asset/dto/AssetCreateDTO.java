package com.smartitsm.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO for creating a new asset.
 */
@Data
public class AssetCreateDTO {
    
    @NotBlank(message = "Asset name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Asset type is required")
    private String assetType;  // SERVER, NETWORK, STORAGE, ENDPOINT, SOFTWARE, CLOUD, DATABASE
    
    private String subType;
    private String manufacturer;
    private String model;
    private String serialNumber;
    
    private String location;
    private String department;
    private String assignedTo;
    private String assignedGroup;
    
    private String ipAddress;
    private String macAddress;
    private String hostname;
    private String operatingSystem;
    private Integer cpuCores;
    private Integer memoryGB;
    private Integer storageGB;
    
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private LocalDate installationDate;
    private Integer expectedLifeYears;
    
    private Double purchaseCost;
    private Double maintenanceCost;
    private Double replacementCost;
    
    private Long parentAssetId;
    private String vendor;
    private String supportLevel;
    private String tags;
}
