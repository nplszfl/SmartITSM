package com.smartitsm.asset.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Asset change history entity - tracks all changes to assets.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_asset_change_history")
public class AssetChangeHistory extends BaseEntity {

    private Long assetId;
    private String assetName;
    private String assetTag;
    private String assetCategory;
    
    // Change info
    private String changeType;          // LOCATION, STATUS, CONFIG, OWNER, MAINTENANCE, DISPOSAL
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    
    // Change details
    private LocalDateTime changedAt;
    private String changedBy;
    private String changedByName;
    private String changeReason;
    
    // Source
    private String source;              // INVENTORY, MANUAL, AUTO_DISCOVERED, TICKET
    private String sourceId;           // Related ticket number or task number
    private String sourceModule;
    
    // Impact assessment
    private String impactLevel;         // LOW, MEDIUM, HIGH, CRITICAL
    private String impactDescription;
    
    // Approval
    private String approvalStatus;     // PENDING, APPROVED, REJECTED
    private String approvedBy;
    private LocalDateTime approvedAt;
    
    // Documentation
    private String changeTicket;
    private String notes;
}