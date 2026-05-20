package com.smartitsm.analytics.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CategoryBreakdown {
    private String category;
    private long count;
    private long resolved;
    private double avgResolutionHours;
    private long breachedCount;
}
