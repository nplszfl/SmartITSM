package com.smartitsm.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowStartDTO {
    private String workflowName;
    private Long ticketId;
    private String ticketNumber;
    private String triggeredBy;
    private Map<String, Object> context;
}
