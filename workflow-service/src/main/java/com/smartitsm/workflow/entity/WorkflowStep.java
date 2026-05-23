package com.smartitsm.workflow.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Workflow step - individual steps within a workflow.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_workflow_step")
public class WorkflowStep extends BaseEntity {

    private Long workflowInstanceId;
    private String stepName;
    private Integer stepOrder;
    private String status;  // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    private String assignee;
    private String assigneeGroup;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    
    private String action;
    private String notes;
    private String outputData;
}
