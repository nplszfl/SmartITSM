package com.smartitsm.workflow.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowService.
 * Tests workflow lifecycle, step transitions, and automation logic.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowInstanceRepository instanceRepository;
    
    @Mock
    private WorkflowStepRepository stepRepository;
    
    @Mock
    private WorkflowDefinitionRepository definitionRepository;
    
    @Mock
    private AIClient aiClient;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(
                instanceRepository, stepRepository, definitionRepository, aiClient);
    }

    @Test
    void startWorkflow_createsInstanceAndSteps() throws Exception {
        // Arrange
        WorkflowStartDTO dto = new WorkflowStartDTO();
        dto.setWorkflowName("incident-resolution");
        dto.setTicketId(1L);
        dto.setTicketNumber("TICKET-001");
        dto.setTriggeredBy("admin");

        WorkflowDefinition definition = WorkflowDefinition.builder()
                .name("incident-resolution")
                .description("Incident resolution workflow")
                .version("1.0")
                .category("TICKET")
                .stepsJson("[{\"name\":\"triage\"},{\"name\":\"resolution\"}]")
                .isActive(true)
                .slaHours("24")
                .createdBy("system")
                .build();

        when(definitionRepository.selectOne(any())).thenReturn(definition);
        when(instanceRepository.insert(any(WorkflowInstance.class))).thenReturn(1);
        when(stepRepository.insert(any(WorkflowStep.class))).thenReturn(1);
        when(stepRepository.selectList(any())).thenReturn(new ArrayList<>());
        when(instanceRepository.selectById(any())).thenReturn(createTestInstance());

        // Act
        WorkflowResponseDTO result = workflowService.startWorkflow(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(instanceRepository).insert(any(WorkflowInstance.class));
        verify(stepRepository, times(2)).insert(any(WorkflowStep.class));
    }

    @Test
    void startWorkflow_withNonExistentDefinition_throwsException() {
        // Arrange
        WorkflowStartDTO dto = new WorkflowStartDTO();
        dto.setWorkflowName("non-existent");
        dto.setTicketId(1L);
        dto.setTicketNumber("TICKET-001");

        when(definitionRepository.selectOne(any())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> workflowService.startWorkflow(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Workflow not found");
    }

    @Test
    void advanceWorkflow_completesCurrentStepAndStartsNext() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");
        instance.setCompletedSteps(0);
        instance.setCurrentStep("triage");

        WorkflowStep currentStep = new WorkflowStep();
        currentStep.setId(1L);
        currentStep.setWorkflowInstanceId(instanceId);
        currentStep.setStepName("triage");
        currentStep.setStatus("RUNNING");
        currentStep.setStartedAt(LocalDateTime.now().minusMinutes(5));

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setId(2L);
        nextStep.setWorkflowInstanceId(instanceId);
        nextStep.setStepName("resolution");
        nextStep.setStatus("PENDING");

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(stepRepository.selectOne(any())).thenReturn(currentStep);
        when(stepRepository.selectList(any())).thenReturn(List.of(nextStep));
        when(stepRepository.updateById(any())).thenReturn(1);
        when(instanceRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.advanceWorkflow(instanceId);

        // Assert
        assertThat(result).isNotNull();
        verify(stepRepository).updateById(any(WorkflowStep.class));
    }

    @Test
    void advanceWorkflow_whenNoMoreSteps_completesWorkflow() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");
        instance.setCompletedSteps(2);
        instance.setTotalSteps(2);

        WorkflowStep currentStep = new WorkflowStep();
        currentStep.setId(1L);
        currentStep.setWorkflowInstanceId(instanceId);
        currentStep.setStepName("final-step");
        currentStep.setStatus("RUNNING");

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(stepRepository.selectOne(any())).thenReturn(currentStep);
        when(stepRepository.selectList(any())).thenReturn(new ArrayList<>());
        when(stepRepository.updateById(any())).thenReturn(1);
        when(instanceRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.advanceWorkflow(instanceId);

        // Assert
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void cancelWorkflow_setsStatusToCancelledAndSkipsSteps() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");
        instance.setStartedAt(LocalDateTime.now().minusHours(1));

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(instanceRepository.updateById(any())).thenReturn(1);
        when(stepRepository.update(any(), any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.cancelWorkflow(instanceId, "Cancelled by user");

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getResult()).contains("CANCELLED");
        verify(stepRepository).update(any(), any());
    }

    @Test
    void getInstance_whenExists_returnsInstance() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);

        // Act
        WorkflowResponseDTO result = workflowService.getInstance(instanceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    void getInstance_whenNotExists_throwsException() {
        // Arrange
        Long instanceId = 999L;
        when(instanceRepository.selectById(instanceId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> workflowService.getInstance(instanceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Workflow instance not found");
    }

    @Test
    void getRunningInstances_returnsOnlyRunningWorkflows() {
        // Arrange
        WorkflowInstance running1 = createTestInstance();
        running1.setStatus("RUNNING");
        
        WorkflowInstance running2 = createTestInstance();
        running2.setStatus("RUNNING");

        when(instanceRepository.selectList(any())).thenReturn(List.of(running1, running2));

        // Act
        List<WorkflowResponseDTO> result = workflowService.getRunningInstances();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getStats_calculatesCorrectStatistics() {
        // Arrange
        when(instanceRepository.selectCount(any())).thenReturn(10L);
        when(instanceRepository.selectList(any())).thenReturn(List.of(createTestInstance()));

        // Act
        WorkflowStatsDTO result = workflowService.getStats();

        // Assert
        assertThat(result.getTotalRunning()).isEqualTo(5L);
        assertThat(result.getTotalCompleted()).isEqualTo(3L);
    }

    @Test
    void getInstancesByTicket_returnsTicketWorkflows() {
        // Arrange
        Long ticketId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setTicketId(ticketId);

        when(instanceRepository.selectList(any())).thenReturn(List.of(instance));

        // Act
        List<WorkflowResponseDTO> result = workflowService.getInstancesByTicket(ticketId);

        // Assert
        assertThat(result).hasSize(1);
    }

    private WorkflowInstance createTestInstance() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .workflowName("incident-resolution")
                .workflowVersion("1.0")
                .status("RUNNING")
                .currentStep("triage")
                .completedSteps(0)
                .totalSteps(3)
                .ticketId(1L)
                .ticketNumber("TICKET-001")
                .triggeredBy("admin")
                .startedAt(LocalDateTime.now())
                .build();
        instance.setId(1L);
        return instance;
    }

    @Test
    void pauseWorkflow_setsStatusToPaused() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");

        WorkflowStep currentStep = new WorkflowStep();
        currentStep.setId(1L);
        currentStep.setWorkflowInstanceId(instanceId);
        currentStep.setStepName("triage");
        currentStep.setStatus("RUNNING");

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(stepRepository.selectOne(any())).thenReturn(currentStep);
        when(instanceRepository.updateById(any())).thenReturn(1);
        when(stepRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.pauseWorkflow(instanceId, "Maintenance window");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PAUSED");
    }

    @Test
    void resumeWorkflow_resumesPausedWorkflow() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("PAUSED");

        WorkflowStep pausedStep = new WorkflowStep();
        pausedStep.setId(1L);
        pausedStep.setWorkflowInstanceId(instanceId);
        pausedStep.setStepName("triage");
        pausedStep.setStatus("PAUSED");
        pausedStep.setStartedAt(LocalDateTime.now().minusMinutes(10));

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setId(2L);
        nextStep.setWorkflowInstanceId(instanceId);
        nextStep.setStepName("resolution");
        nextStep.setStatus("PENDING");

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(stepRepository.selectOne(any())).thenReturn(pausedStep);
        when(stepRepository.selectList(any())).thenReturn(List.of(nextStep));
        when(instanceRepository.updateById(any())).thenReturn(1);
        when(stepRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.resumeWorkflow(instanceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void failWorkflow_marksInstanceAsFailed() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");
        instance.setStartedAt(LocalDateTime.now().minusMinutes(30));

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(instanceRepository.updateById(any())).thenReturn(1);
        when(stepRepository.update(any(), any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.failWorkflow(instanceId, "External dependency unavailable");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void skipStep_marksCurrentStepAsSkipped() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("RUNNING");

        WorkflowStep currentStep = new WorkflowStep();
        currentStep.setId(1L);
        currentStep.setWorkflowInstanceId(instanceId);
        currentStep.setStepName("triage");
        currentStep.setStatus("RUNNING");
        currentStep.setStartedAt(LocalDateTime.now().minusMinutes(5));

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setId(2L);
        nextStep.setWorkflowInstanceId(instanceId);
        nextStep.setStepName("resolution");
        nextStep.setStatus("PENDING");

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(stepRepository.selectOne(any())).thenReturn(currentStep);
        when(stepRepository.selectList(any())).thenReturn(List.of(nextStep));
        when(stepRepository.updateById(any())).thenReturn(1);
        when(instanceRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.skipStep(instanceId, "Not applicable for this ticket");

        // Assert
        assertThat(result).isNotNull();
        verify(stepRepository).updateById(any(WorkflowStep.class));
    }

    @Test
    void getWorkflowsByStatus_returnsFilteredWorkflows() {
        // Arrange
        WorkflowInstance cancelled = createTestInstance();
        cancelled.setStatus("CANCELLED");

        when(instanceRepository.selectList(any())).thenReturn(List.of(cancelled));

        // Act
        List<WorkflowResponseDTO> result = workflowService.getWorkflowsByStatus("CANCELLED");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void retryWorkflow_restartsCancelledWorkflow() {
        // Arrange
        Long instanceId = 1L;
        WorkflowInstance instance = createTestInstance();
        instance.setId(instanceId);
        instance.setStatus("CANCELLED");
        instance.setCompletedSteps(1);

        WorkflowStep skippedStep = new WorkflowStep();
        skippedStep.setId(1L);
        skippedStep.setWorkflowInstanceId(instanceId);
        skippedStep.setStepName("triage");
        skippedStep.setStatus("COMPLETED");

        WorkflowStep pendingStep = new WorkflowStep();
        pendingStep.setId(2L);
        pendingStep.setWorkflowInstanceId(instanceId);
        pendingStep.setStepName("resolution");
        pendingStep.setStatus("SKIPPED");

        WorkflowDefinition definition = WorkflowDefinition.builder()
                .name("incident-resolution")
                .version("1.0")
                .stepsJson("[{\"name\":\"triage\"},{\"name\":\"resolution\"}]")
                .isActive(true)
                .build();

        when(instanceRepository.selectById(instanceId)).thenReturn(instance);
        when(definitionRepository.selectOne(any())).thenReturn(definition);
        when(stepRepository.selectList(any())).thenReturn(List.of(skippedStep, pendingStep));
        when(stepRepository.updateById(any())).thenReturn(1);
        when(instanceRepository.updateById(any())).thenReturn(1);

        // Act
        WorkflowResponseDTO result = workflowService.retryWorkflow(instanceId);

        // Assert
        assertThat(result).isNotNull();
        verify(instanceRepository).updateById(any(WorkflowInstance.class));
        verify(stepRepository, atLeastOnce()).updateById(any(WorkflowStep.class));
    }
}
