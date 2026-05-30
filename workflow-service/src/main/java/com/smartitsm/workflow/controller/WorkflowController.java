package com.smartitsm.workflow.controller;

import com.smartitsm.common.dto.ApiResponse;
import com.smartitsm.workflow.dto.WorkflowResponseDTO;
import com.smartitsm.workflow.dto.WorkflowStartDTO;
import com.smartitsm.workflow.dto.WorkflowStatsDTO;
import com.smartitsm.workflow.entity.WorkflowDefinition;
import com.smartitsm.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Workflow REST API Controller.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/start")
    public ApiResponse<WorkflowResponseDTO> startWorkflow(@RequestBody WorkflowStartDTO dto) {
        return ApiResponse.ok(workflowService.startWorkflow(dto));
    }

    @GetMapping("/{instanceId}")
    public ApiResponse<WorkflowResponseDTO> getInstance(@PathVariable Long instanceId) {
        return ApiResponse.ok(workflowService.getInstance(instanceId));
    }

    @PostMapping("/{instanceId}/advance")
    public ApiResponse<WorkflowResponseDTO> advanceWorkflow(@PathVariable Long instanceId) {
        return ApiResponse.ok(workflowService.advanceWorkflow(instanceId));
    }

    @PostMapping("/{instanceId}/complete")
    public ApiResponse<WorkflowResponseDTO> completeStep(
            @PathVariable Long instanceId,
            @RequestParam String action,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String outputData) {
        return ApiResponse.ok(workflowService.completeStep(instanceId, action, notes, outputData));
    }

    @PostMapping("/{instanceId}/cancel")
    public ApiResponse<WorkflowResponseDTO> cancelWorkflow(
            @PathVariable Long instanceId,
            @RequestParam String reason) {
        return ApiResponse.ok(workflowService.cancelWorkflow(instanceId, reason));
    }

    @PostMapping("/{instanceId}/pause")
    public ApiResponse<WorkflowResponseDTO> pauseWorkflow(
            @PathVariable Long instanceId,
            @RequestParam String reason) {
        return ApiResponse.ok(workflowService.pauseWorkflow(instanceId, reason));
    }

    @PostMapping("/{instanceId}/resume")
    public ApiResponse<WorkflowResponseDTO> resumeWorkflow(@PathVariable Long instanceId) {
        return ApiResponse.ok(workflowService.resumeWorkflow(instanceId));
    }

    @PostMapping("/{instanceId}/fail")
    public ApiResponse<WorkflowResponseDTO> failWorkflow(
            @PathVariable Long instanceId,
            @RequestParam String errorMessage) {
        return ApiResponse.ok(workflowService.failWorkflow(instanceId, errorMessage));
    }

    @PostMapping("/{instanceId}/retry")
    public ApiResponse<WorkflowResponseDTO> retryWorkflow(@PathVariable Long instanceId) {
        return ApiResponse.ok(workflowService.retryWorkflow(instanceId));
    }

    @PostMapping("/{instanceId}/skip-step")
    public ApiResponse<WorkflowResponseDTO> skipStep(
            @PathVariable Long instanceId,
            @RequestParam String reason) {
        return ApiResponse.ok(workflowService.skipStep(instanceId, reason));
    }

    @GetMapping("/ticket/{ticketId}")
    public ApiResponse<List<WorkflowResponseDTO>> getByTicket(@PathVariable Long ticketId) {
        return ApiResponse.ok(workflowService.getInstancesByTicket(ticketId));
    }

    @GetMapping("/running")
    public ApiResponse<List<WorkflowResponseDTO>> getRunning() {
        return ApiResponse.ok(workflowService.getRunningInstances());
    }

    @GetMapping("/stats")
    public ApiResponse<WorkflowStatsDTO> getStats() {
        return ApiResponse.ok(workflowService.getStats());
    }

    @GetMapping("/definitions")
    public ApiResponse<List<WorkflowDefinition>> listDefinitions(
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(workflowService.listDefinitions(category));
    }

    @GetMapping("/definitions/{name}")
    public ApiResponse<WorkflowDefinition> getDefinition(@PathVariable String name) {
        return ApiResponse.ok(workflowService.getDefinition(name));
    }

    @PostMapping("/definitions")
    public ApiResponse<WorkflowDefinition> createDefinition(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) payload.get("steps");
        WorkflowDefinition def = workflowService.createDefinition(
                (String) payload.get("name"),
                (String) payload.get("description"),
                (String) payload.get("category"),
                steps,
                (String) payload.get("slaHours"),
                (String) payload.get("createdBy")
        );
        return ApiResponse.ok(def);
    }

    @GetMapping("/{instanceId}/optimize")
    public ApiResponse<?> optimizeWorkflow(@PathVariable Long instanceId) {
        // Get instance name then optimize
        WorkflowResponseDTO instance = workflowService.getInstance(instanceId);
        return ApiResponse.ok(workflowService.optimizeWorkflow(instance.getWorkflowName()));
    }
}
