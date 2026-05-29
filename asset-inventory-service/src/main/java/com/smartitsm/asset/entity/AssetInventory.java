package com.smartitsm.asset.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Asset inventory record - tracks each asset's inventory status.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_asset_inventory")
public class AssetInventory extends BaseEntity {

    private Long taskId;
    private String taskNumber;
    
    // Asset info
    private Long assetId;
    private String assetName;
    private String assetTag;
    private String assetCategory;
    private String serialNumber;
    
    // Location
    private String location;
    private String building;
    private String floor;
    private String room;
    
    // Inventory status
    private String status;               // VERIFIED, MISSING, CHANGED, NEW, UNKNOWN
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    
    // Previous vs current state
    private String previousLocation;
    private String currentLocation;
    private String previousStatus;
    private String currentStatus;
    
    // Change tracking
    private Boolean hasChange;
    private String changeType;          // LOCATION, STATUS, CONFIG, OWNER
    private String changeDescription;
    private LocalDateTime changedAt;
    
    // Discrepancy
    private String discrepancyType;     // MISSING, FOUND, MOVED, MODIFIED
    private String discrepancyNotes;
    private Integer discrepancySeverity; // 1-5
    
    // Asset details snapshot
    private String manufacturer;
    private String model;
    private String purchaseDate;
    private String warrantyExpiry;
    
    // Photo evidence
    private String photoUrl;
    private String notes;
}