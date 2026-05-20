package com.smartitsm.analytics.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AssetHealthSummary {
    private long total;
    private long healthy;
    private long warning;
    private long critical;
    private long byTypeSERVER;
    private long byTypeNETWORK;
    private long byTypeSTORAGE;
    private long byTypeENDPOINT;
    private long byTypeCLOUD;
    private long byTypeDATABASE;
    private long warrantyExpiring;
    private long atRisk;
}
