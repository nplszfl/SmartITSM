package com.smartitsm.asset.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for inventory task creation.
 */
@Data
@Builder
public class InventoryTaskCreateDTO {

    private String taskType;           // FULL, PARTIAL, SPOT_CHECK
    private String scheduleType;       // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, ONE_TIME
    private LocalDateTime scheduledAt;
    private String assetCategory;
    private String assetLocation;
    private Integer targetAssetCount;
    private String assignedTo;
    private String assignedGroup;
    private String notes;
}