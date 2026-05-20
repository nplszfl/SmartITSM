package com.smartitsm.workflow.dto;

import lombok.Data;

@Data
public class WorkflowStatsDTO {
    private long totalRunning;
    private long totalCompleted;
    private long totalFailed;
    private long avgDurationSeconds;
    private long activeToday;
}
