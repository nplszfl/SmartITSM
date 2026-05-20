package com.smartitsm.workflow.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Workflow definition - template for workflow instances.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_workflow_definition")
public class WorkflowDefinition extends BaseEntity {

    private String name;
    private String description;
    private String version;
    private String category;  // TICKET, ASSET, CHANGE, etc.
    private String stepsJson;  // JSON array of step definitions
    private Boolean isActive;
    private String slaHours;
    private String createdBy;
}
