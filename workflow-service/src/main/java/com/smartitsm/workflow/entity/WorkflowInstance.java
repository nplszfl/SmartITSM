package com.smartitsm.workflow.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Workflow instance - represents a running or completed workflow.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_workflow_instance")
public class WorkflowInstance extends BaseEntity {

    private String workflowName;
    private String workflowVersion;
    private String status;  // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private String currentStep;
    private Integer totalSteps;
    private Integer completedSteps;
    
    // Context
    private Long ticketId;
    private String ticketNumber;
    private String triggeredBy;
    
    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    
    // Result
    private String result;
    private String errorMessage;
    private String outputData;
}
