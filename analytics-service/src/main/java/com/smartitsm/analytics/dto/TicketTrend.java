package com.smartitsm.analytics.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class TicketTrend {
    private String date;
    private long created;
    private long resolved;
    private long breached;
}
