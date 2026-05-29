package com.smartitsm.asset.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Inventory task entity for scheduled asset inventory checks.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_inventory_task")
public class InventoryTask extends BaseEntity {

    private String taskNumber;           // Unique task reference (e.g., INV-20240529-0001)
    
    // Task type and status
    private String taskType;             // FULL, PARTIAL, SPOT_CHECK
    private String status;               // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    
    // Schedule info
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String scheduleType;        // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, ONE_TIME
    
    // Scope
    private String assetCategory;       // ALL, SERVERS, NETWORK, ENDUSER, etc.
    private String assetLocation;
    private Integer targetAssetCount;
    private Integer actualAssetCount;
    
    // Assigned team
    private String assignedTo;
    private String assignedGroup;
    
    // Results summary
    private Integer totalAssetsFound;
    private Integer totalAssetsMissing;
    private Integer totalAssetsChanged;
    private Integer totalDiscrepancies;
    
    // Notes
    private String notes;
    private String completionNotes;
    
    // Metadata
    private String triggerSource;       // SCHEDULE, MANUAL, EVENT
    private String createdBy;
}