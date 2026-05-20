package com.smartitsm.workflow.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkflowResponseDTO {
    private Long instanceId;
    private String workflowName;
    private String status;
    private String currentStep;
    private Integer completedSteps;
    private Integer totalSteps;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private String result;
}
