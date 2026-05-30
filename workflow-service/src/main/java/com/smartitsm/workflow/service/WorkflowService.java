package com.smartitsm.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartitsm.common.ai.AIClient;
import com.smartitsm.common.dto.AIInsightResult;
import com.smartitsm.common.exception.BusinessException;
import com.smartitsm.workflow.dto.WorkflowResponseDTO;
import com.smartitsm.workflow.dto.WorkflowStartDTO;
import com.smartitsm.workflow.dto.WorkflowStatsDTO;
import com.smartitsm.workflow.entity.WorkflowDefinition;
import com.smartitsm.workflow.entity.WorkflowInstance;
import com.smartitsm.workflow.entity.WorkflowStep;
import com.smartitsm.workflow.repository.WorkflowDefinitionRepository;
import com.smartitsm.workflow.repository.WorkflowInstanceRepository;
import com.smartitsm.workflow.repository.WorkflowStepRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Workflow Service - core business logic for ITSM workflow automation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final AIClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Workflow Lifecycle ====================

    /**
     * Start a new workflow instance.
     */
    @Transactional
    public WorkflowResponseDTO startWorkflow(WorkflowStartDTO dto) {
        log.info("Starting workflow: {} for ticket {}", dto.getWorkflowName(), dto.getTicketNumber());

        // Find workflow definition
        WorkflowDefinition definition = findActiveDefinition(dto.getWorkflowName());
        if (definition == null) {
            throw new BusinessException("WORKFLOW_NOT_FOUND", "Workflow not found: " + dto.getWorkflowName());
        }

        // Parse steps
        List<Map<String, Object>> stepsDef;
        try {
            stepsDef = objectMapper.readValue(definition.getStepsJson(), 
                new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BusinessException("INVALID_WORKFLOW", "Workflow definition is invalid: " + e.getMessage());
        }

        // Create workflow instance
        WorkflowInstance instance = WorkflowInstance.builder()
                .workflowName(definition.getName())
                .workflowVersion(definition.getVersion())
                .status("RUNNING")
                .currentStep(stepsDef.isEmpty() ? null : (String) stepsDef.get(0).get("name"))
                .totalSteps(stepsDef.size())
                .completedSteps(0)
                .ticketId(dto.getTicketId())
                .ticketNumber(dto.getTicketNumber())
                .triggeredBy(dto.getTriggeredBy())
                .startedAt(LocalDateTime.now())
                .build();

        instanceRepository.insert(instance);

        // Create workflow steps
        for (int i = 0; i < stepsDef.size(); i++) {
            Map<String, Object> stepDef = stepsDef.get(i);
            WorkflowStep step = WorkflowStep.builder()
                    .workflowInstanceId(instance.getId())
                    .stepName((String) stepDef.get("name"))
                    .stepOrder(i + 1)
                    .status("PENDING")
                    .assignee((String) stepDef.get("assignee"))
                    .assigneeGroup((String) stepDef.get("assigneeGroup"))
                    .action((String) stepDef.get("action"))
                    .build();
            stepRepository.insert(step);
        }

        // Start first step
        advanceWorkflow(instance.getId());

        log.info("Workflow {} started with instance ID {}", dto.getWorkflowName(), instance.getId());
        return toResponseDTO(instance);
    }

    /**
     * Advance workflow to next step.
     */
    @Transactional
    public WorkflowResponseDTO advanceWorkflow(Long instanceId) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        if (!"RUNNING".equals(instance.getStatus())) {
            throw new BusinessException("WORKFLOW_NOT_RUNNING", "Workflow is not running: " + instance.getStatus());
        }

        // Mark current step as completed
        WorkflowStep currentStep = findCurrentStep(instanceId);
        if (currentStep != null && "RUNNING".equals(currentStep.getStatus())) {
            currentStep.setStatus("COMPLETED");
            currentStep.setCompletedAt(LocalDateTime.now());
            currentStep.setDurationSeconds(
                ChronoUnit.SECONDS.between(currentStep.getStartedAt(), LocalDateTime.now()));
            stepRepository.updateById(currentStep);
            instance.setCompletedSteps(instance.getCompletedSteps() + 1);
        }

        // Find next step
        List<WorkflowStep> pendingSteps = stepRepository.selectList(
            new QueryWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .eq("status", "PENDING")
                .orderByAsc("step_order"));

        if (pendingSteps.isEmpty()) {
            // Workflow complete
            instance.setStatus("COMPLETED");
            instance.setCompletedAt(LocalDateTime.now());
            instance.setDurationSeconds(
                ChronoUnit.SECONDS.between(instance.getStartedAt(), LocalDateTime.now()));
            instance.setResult("SUCCESS");
            instanceRepository.updateById(instance);
            log.info("Workflow {} completed successfully", instanceId);
        } else {
            // Start next step
            WorkflowStep nextStep = pendingSteps.get(0);
            nextStep.setStatus("RUNNING");
            nextStep.setStartedAt(LocalDateTime.now());
            stepRepository.updateById(nextStep);
            instance.setCurrentStep(nextStep.getStepName());
            instanceRepository.updateById(instance);
            log.info("Workflow {} advanced to step: {}", instanceId, nextStep.getStepName());
        }

        return toResponseDTO(instance);
    }

    /**
     * Complete a step manually (with action/notes).
     */
    @Transactional
    public WorkflowResponseDTO completeStep(Long instanceId, String action, String notes, String outputData) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        WorkflowStep currentStep = findCurrentStep(instanceId);
        if (currentStep != null) {
            currentStep.setStatus("COMPLETED");
            currentStep.setCompletedAt(LocalDateTime.now());
            currentStep.setDurationSeconds(
                ChronoUnit.SECONDS.between(currentStep.getStartedAt(), LocalDateTime.now()));
            currentStep.setAction(action);
            currentStep.setNotes(notes);
            currentStep.setOutputData(outputData);
            stepRepository.updateById(currentStep);
            instance.setCompletedSteps(instance.getCompletedSteps() + 1);
        }

        // Advance to next
        return advanceWorkflow(instanceId);
    }

    /**
     * Cancel a workflow instance.
     */
    @Transactional
    public WorkflowResponseDTO cancelWorkflow(Long instanceId, String reason) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        instance.setStatus("CANCELLED");
        instance.setCompletedAt(LocalDateTime.now());
        instance.setDurationSeconds(
            ChronoUnit.SECONDS.between(instance.getStartedAt(), LocalDateTime.now()));
        instance.setResult("CANCELLED: " + reason);
        instanceRepository.updateById(instance);

        // Mark pending steps as skipped
        stepRepository.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .eq("status", "PENDING")
                .set("status", "SKIPPED"));

        log.info("Workflow {} cancelled: {}", instanceId, reason);
        return toResponseDTO(instance);
    }

    /**
     * Pause a running workflow instance.
     */
    @Transactional
    public WorkflowResponseDTO pauseWorkflow(Long instanceId, String reason) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        if (!"RUNNING".equals(instance.getStatus())) {
            throw new BusinessException("WORKFLOW_NOT_RUNNING", "Workflow is not running: " + instance.getStatus());
        }

        instance.setStatus("PAUSED");
        instance.setResult("PAUSED: " + reason);
        instanceRepository.updateById(instance);

        // Mark current step as paused
        WorkflowStep currentStep = findCurrentStep(instanceId);
        if (currentStep != null) {
            currentStep.setStatus("PAUSED");
            stepRepository.updateById(currentStep);
        }

        log.info("Workflow {} paused: {}", instanceId, reason);
        return toResponseDTO(instance);
    }

    /**
     * Resume a paused workflow instance.
     */
    @Transactional
    public WorkflowResponseDTO resumeWorkflow(Long instanceId) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        if (!"PAUSED".equals(instance.getStatus())) {
            throw new BusinessException("WORKFLOW_NOT_PAUSED", "Workflow is not paused: " + instance.getStatus());
        }

        instance.setStatus("RUNNING");
        instance.setResult(null);
        instanceRepository.updateById(instance);

        // Resume the paused step
        WorkflowStep pausedStep = stepRepository.selectOne(
            new QueryWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .eq("status", "PAUSED")
                .last("LIMIT 1"));
        
        if (pausedStep != null) {
            pausedStep.setStatus("RUNNING");
            pausedStep.setStartedAt(LocalDateTime.now());
            stepRepository.updateById(pausedStep);
        }

        log.info("Workflow {} resumed", instanceId);
        return toResponseDTO(instance);
    }

    /**
     * Fail a workflow instance.
     */
    @Transactional
    public WorkflowResponseDTO failWorkflow(Long instanceId, String errorMessage) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        instance.setStatus("FAILED");
        instance.setCompletedAt(LocalDateTime.now());
        instance.setDurationSeconds(
            ChronoUnit.SECONDS.between(instance.getStartedAt(), LocalDateTime.now()));
        instance.setResult("FAILED");
        instance.setErrorMessage(errorMessage);
        instanceRepository.updateById(instance);

        // Mark pending steps as skipped
        stepRepository.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .eq("status", "PENDING")
                .set("status", "SKIPPED"));

        log.info("Workflow {} failed: {}", instanceId, errorMessage);
        return toResponseDTO(instance);
    }

    /**
     * Retry a failed or cancelled workflow.
     */
    @Transactional
    public WorkflowResponseDTO retryWorkflow(Long instanceId) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        if (!"FAILED".equals(instance.getStatus()) && !"CANCELLED".equals(instance.getStatus())) {
            throw new BusinessException("INVALID_STATUS", "Can only retry failed or cancelled workflows: " + instance.getStatus());
        }

        // Get workflow definition
        WorkflowDefinition definition = findActiveDefinition(instance.getWorkflowName());
        if (definition == null) {
            throw new BusinessException("WORKFLOW_NOT_FOUND", "Workflow definition not found: " + instance.getWorkflowName());
        }

        // Reset instance
        instance.setStatus("RUNNING");
        instance.setCurrentStep(null);
        instance.setCompletedSteps(0);
        instance.setStartedAt(LocalDateTime.now());
        instance.setCompletedAt(null);
        instance.setDurationSeconds(null);
        instance.setResult(null);
        instance.setErrorMessage(null);
        instanceRepository.updateById(instance);

        // Reset all steps to PENDING
        List<WorkflowStep> allSteps = stepRepository.selectList(
            new QueryWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .orderByAsc("step_order"));
        
        for (WorkflowStep step : allSteps) {
            step.setStatus("PENDING");
            step.setStartedAt(null);
            step.setCompletedAt(null);
            step.setDurationSeconds(null);
            step.setNotes(null);
            step.setOutputData(null);
            stepRepository.updateById(step);
        }

        // Start first step
        advanceWorkflow(instance.getId());

        log.info("Workflow {} retried", instanceId);
        return toResponseDTO(instance);
    }

    /**
     * Skip the current step and move to next.
     */
    @Transactional
    public WorkflowResponseDTO skipStep(Long instanceId, String reason) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }

        if (!"RUNNING".equals(instance.getStatus())) {
            throw new BusinessException("WORKFLOW_NOT_RUNNING", "Workflow is not running: " + instance.getStatus());
        }

        WorkflowStep currentStep = findCurrentStep(instanceId);
        if (currentStep != null) {
            currentStep.setStatus("SKIPPED");
            currentStep.setCompletedAt(LocalDateTime.now());
            currentStep.setNotes("SKIPPED: " + reason);
            currentStep.setDurationSeconds(
                currentStep.getStartedAt() != null ? 
                    ChronoUnit.SECONDS.between(currentStep.getStartedAt(), LocalDateTime.now()) : 0);
            stepRepository.updateById(currentStep);
            instance.setCompletedSteps(instance.getCompletedSteps() + 1);
        }

        log.info("Workflow {} step skipped: {}", instanceId, reason);
        return advanceWorkflow(instanceId);
    }

    /**
     * Get workflows by status.
     */
    public List<WorkflowResponseDTO> getWorkflowsByStatus(String status) {
        QueryWrapper<WorkflowInstance> query = new QueryWrapper<>();
        if (status != null) {
            query.eq("status", status);
        }
        query.orderByDesc("started_at").last("LIMIT 100");
        return instanceRepository.selectList(query).stream().map(this::toResponseDTO).toList();
    }

    /**
     * Get workflow steps history for an instance.
     */
    public List<WorkflowStep> getWorkflowSteps(Long instanceId) {
        return stepRepository.selectList(
            new QueryWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .orderByAsc("step_order"));
    }

    // ==================== Workflow Definitions ====================

    /**
     * Create a new workflow definition.
     */
    @Transactional
    public WorkflowDefinition createDefinition(String name, String description, String category,
            List<Map<String, Object>> steps, String slaHours, String createdBy) {
        WorkflowDefinition existing = findActiveDefinition(name);
        String version = existing == null ? "1.0" : incrementVersion(existing.getVersion());

        List<Map<String, Object>> stepsWithDefaults = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            step.putIfAbsent("order", i + 1);
            stepsWithDefaults.add(step);
        }

        String stepsJson;
        try {
            stepsJson = objectMapper.writeValueAsString(stepsWithDefaults);
        } catch (Exception e) {
            throw new BusinessException("INVALID_STEPS", "Failed to serialize steps: " + e.getMessage());
        }

        WorkflowDefinition definition = WorkflowDefinition.builder()
                .name(name)
                .description(description)
                .version(version)
                .category(category)
                .stepsJson(stepsJson)
                .isActive(true)
                .slaHours(slaHours)
                .createdBy(createdBy)
                .build();

        definitionRepository.insert(definition);
        log.info("Created workflow definition: {} v{}", name, version);
        return definition;
    }

    /**
     * Get workflow definition by name (latest active version).
     */
    public WorkflowDefinition getDefinition(String name) {
        return findActiveDefinition(name);
    }

    /**
     * List all active workflow definitions.
     */
    public List<WorkflowDefinition> listDefinitions(String category) {
        QueryWrapper<WorkflowDefinition> query = new QueryWrapper<>();
        query.eq("is_active", true);
        if (category != null) query.eq("category", category);
        query.orderByDesc("updated_at");
        return definitionRepository.selectList(query);
    }

    // ==================== Monitoring & Analytics ====================

    /**
     * Get workflow instance by ID.
     */
    public WorkflowResponseDTO getInstance(Long instanceId) {
        WorkflowInstance instance = instanceRepository.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("INSTANCE_NOT_FOUND", "Workflow instance not found: " + instanceId);
        }
        return toResponseDTO(instance);
    }

    /**
     * Get workflow instances by ticket.
     */
    public List<WorkflowResponseDTO> getInstancesByTicket(Long ticketId) {
        return instanceRepository.selectList(
            new QueryWrapper<WorkflowInstance>().eq("ticket_id", ticketId)
        ).stream().map(this::toResponseDTO).toList();
    }

    /**
     * Get running workflow instances.
     */
    public List<WorkflowResponseDTO> getRunningInstances() {
        return instanceRepository.selectList(
            new QueryWrapper<WorkflowInstance>().eq("status", "RUNNING")
                .orderByDesc("started_at")
                .last("LIMIT 100")
        ).stream().map(this::toResponseDTO).toList();
    }

    /**
     * Get workflow statistics.
     */
    public WorkflowStatsDTO getStats() {
        WorkflowStatsDTO stats = new WorkflowStatsDTO();
        stats.setTotalRunning(instanceRepository.selectCount(
            new QueryWrapper<WorkflowInstance>().eq("status", "RUNNING")));
        stats.setTotalCompleted(instanceRepository.selectCount(
            new QueryWrapper<WorkflowInstance>().eq("status", "COMPLETED")));
        stats.setTotalFailed(instanceRepository.selectCount(
            new QueryWrapper<WorkflowInstance>().eq("status", "FAILED")));

        // Avg duration for completed workflows
        List<WorkflowInstance> completed = instanceRepository.selectList(
            new QueryWrapper<WorkflowInstance>().eq("status", "COMPLETED")
                .isNotNull("duration_seconds"));
        if (!completed.isEmpty()) {
            long avg = completed.stream()
                .mapToLong(i -> i.getDurationSeconds() != null ? i.getDurationSeconds() : 0)
                .sum() / completed.size();
            stats.setAvgDurationSeconds(avg);
        }

        // Active today
        stats.setActiveToday(instanceRepository.selectCount(
            new QueryWrapper<WorkflowInstance>()
                .ge("started_at", LocalDateTime.now().toLocalDate().atStartOfDay())));

        return stats;
    }

    /**
     * Optimize workflow using AI.
     */
    public AIInsightResult optimizeWorkflow(String workflowName) {
        WorkflowDefinition definition = findActiveDefinition(workflowName);
        if (definition == null) {
            throw new BusinessException("WORKFLOW_NOT_FOUND", "Workflow not found: " + workflowName);
        }

        // Get historical data
        List<WorkflowInstance> instances = instanceRepository.selectList(
            new QueryWrapper<WorkflowInstance>()
                .eq("workflow_name", workflowName)
                .eq("status", "COMPLETED")
                .last("LIMIT 50"));

        double avgHours = 0;
        if (!instances.isEmpty()) {
            avgHours = instances.stream()
                .mapToLong(i -> i.getDurationSeconds() != null ? i.getDurationSeconds() / 3600 : 0)
                .average().orElse(0);
        }

        AIClient.WorkflowOptimizationContext context = AIClient.WorkflowOptimizationContext.builder()
                .workflowName(workflowName)
                .totalSteps(definition.getStepsJson() != null ? 
                    definition.getStepsJson().split("stepName", -1).length - 1 : 0)
                .avgCompletionHours(avgHours)
                .slaComplianceRate(95.0)
                .currentCost(0.0)
                .stepDetails(List.of())
                .bottlenecks(List.of())
                .build();

        return aiClient.optimizeWorkflow(context);
    }

    // ==================== Helper Methods ====================

    private WorkflowDefinition findActiveDefinition(String name) {
        return definitionRepository.selectOne(
            new QueryWrapper<WorkflowDefinition>()
                .eq("name", name)
                .eq("is_active", true)
                .orderByDesc("version")
                .last("LIMIT 1"));
    }

    private WorkflowStep findCurrentStep(Long instanceId) {
        return stepRepository.selectOne(
            new QueryWrapper<WorkflowStep>()
                .eq("workflow_instance_id", instanceId)
                .eq("status", "RUNNING")
                .last("LIMIT 1"));
    }

    private String incrementVersion(String version) {
        String[] parts = version.split("[.]");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        return major + "." + (minor + 1);
    }

    private WorkflowResponseDTO toResponseDTO(WorkflowInstance instance) {
        return WorkflowResponseDTO.builder()
                .instanceId(instance.getId())
                .workflowName(instance.getWorkflowName())
                .status(instance.getStatus())
                .currentStep(instance.getCurrentStep())
                .completedSteps(instance.getCompletedSteps())
                .totalSteps(instance.getTotalSteps())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .durationSeconds(instance.getDurationSeconds())
                .result(instance.getResult())
                .build();
    }
}
