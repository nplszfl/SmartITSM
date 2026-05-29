package com.smartitsm.asset.dto;

import lombok.Data;
import lombok.Builder;

/**
 * DTO for asset change history request.
 */
@Data
@Builder
public class AssetChangeHistoryDTO {

    private Long assetId;
    private String assetName;
    private String assetTag;
    private String assetCategory;
    private String changeType;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private String changedByName;
    private String changeReason;
    private String source;
    private String sourceId;
    private String impactLevel;
}
