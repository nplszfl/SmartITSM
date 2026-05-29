package com.smartitsm.asset.service;

import com.smartitsm.asset.dto.AssetChangeHistoryDTO;
import com.smartitsm.asset.dto.AssetUsageStatsDTO;
import com.smartitsm.asset.dto.InventoryTaskCreateDTO;
import com.smartitsm.asset.entity.AssetChangeHistory;
import com.smartitsm.asset.entity.AssetInventory;
import com.smartitsm.asset.entity.InventoryTask;
import com.smartitsm.asset.repository.AssetChangeHistoryRepository;
import com.smartitsm.asset.repository.AssetInventoryRepository;
import com.smartitsm.asset.repository.InventoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetInventoryService.
 */
@ExtendWith(MockitoExtension.class)
class AssetInventoryServiceTest {

    @Mock
    private InventoryTaskRepository taskRepository;

    @Mock
    private AssetInventoryRepository inventoryRepository;

    @Mock
    private AssetChangeHistoryRepository changeHistoryRepository;

    @InjectMocks
    private AssetInventoryService inventoryService;

    private InventoryTaskCreateDTO testTaskDTO;
    private AssetChangeHistoryDTO testChangeDTO;

    @BeforeEach
    void setUp() {
        testTaskDTO = InventoryTaskCreateDTO.builder()
            .taskType("FULL")
            .scheduleType("MONTHLY")
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .assetCategory("SERVERS")
            .assetLocation("DC-1")
            .targetAssetCount(100)
            .assignedTo("admin001")
            .assignedGroup("Asset_Team")
            .notes("Monthly server inventory check")
            .build();

        testChangeDTO = AssetChangeHistoryDTO.builder()
            .assetId(1L)
            .assetName("Web Server 01")
            .assetTag("SRV-001")
            .assetCategory("SERVERS")
            .changeType("LOCATION")
            .fieldChanged("location")
            .oldValue("DC-1-Floor-1")
            .newValue("DC-1-Floor-2")
            .changedBy("admin001")
            .changedByName("System Admin")
            .changeReason("Reorganization")
            .source("MANUAL")
            .impactLevel("LOW")
            .build();
    }

    @Test
    void testCreateInventoryTask() {
        // Arrange
        when(taskRepository.save(any(InventoryTask.class)))
            .thenAnswer(invocation -> {
                InventoryTask task = invocation.getArgument(0);
                task.setId(1L);
                return task;
            });

        // Act
        InventoryTask result = inventoryService.createInventoryTask(testTaskDTO);

        // Assert
        assertNotNull(result);
        assertEquals("FULL", result.getTaskType());
        assertEquals("MONTHLY", result.getScheduleType());
        assertEquals("PENDING", result.getStatus());
        assertEquals("SERVERS", result.getAssetCategory());
        assertEquals("DC-1", result.getAssetLocation());
        assertEquals(100, result.getTargetAssetCount());
        assertEquals("MANUAL", result.getTriggerSource());
        verify(taskRepository, times(1)).save(any(InventoryTask.class));
    }

    @Test
    void testStartTask() {
        // Arrange
        InventoryTask task = InventoryTask.builder()
            .id(1L)
            .taskNumber("INV-20240529-0001")
            .taskType("FULL")
            .status("PENDING")
            .build();

        when(taskRepository.getById(1L)).thenReturn(task);
        when(taskRepository.updateById(any(InventoryTask.class))).thenReturn(true);

        // Act
        InventoryTask result = inventoryService.startTask(1L);

        // Assert
        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(taskRepository, times(1)).updateById(any(InventoryTask.class));
    }

    @Test
    void testStartTask_NotFound() {
        // Arrange
        when(taskRepository.getById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> inventoryService.startTask(999L));
    }

    @Test
    void testCompleteTask() {
        // Arrange
        InventoryTask task = InventoryTask.builder()
            .id(1L)
            .taskNumber("INV-20240529-0001")
            .taskType("FULL")
            .status("IN_PROGRESS")
            .startedAt(LocalDateTime.now().minusHours(2))
            .build();

        when(taskRepository.getById(1L)).thenReturn(task);
        when(inventoryRepository.list(any())).thenReturn(java.util.List.of());
        when(taskRepository.updateById(any(InventoryTask.class))).thenReturn(true);

        // Act
        InventoryTask result = inventoryService.completeTask(1L, "All assets verified successfully");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals("All assets verified successfully", result.getCompletionNotes());
        verify(taskRepository, times(1)).updateById(any(InventoryTask.class));
    }

    @Test
    void testGetAssetUsageStats() {
        // Act
        AssetUsageStatsDTO stats = inventoryService.getAssetUsageStats("WEEKLY");

        // Assert
        assertNotNull(stats);
        assertEquals(500L, stats.getTotalAssets());
        assertEquals(450L, stats.getActiveAssets());
        assertEquals(78.5, stats.getOverallUtilizationRate());
        assertEquals(100L, stats.getServersCount());
        assertEquals(85.0, stats.getServersUtilization());
        assertEquals("WEEKLY", stats.getTrendPeriod());
    }

    @Test
    void testRecordAssetChange() {
        // Arrange
        when(changeHistoryRepository.save(any(AssetChangeHistory.class)))
            .thenAnswer(invocation -> {
                AssetChangeHistory history = invocation.getArgument(0);
                history.setId(1L);
                return history;
            });

        // Act
        AssetChangeHistory result = inventoryService.recordAssetChange(testChangeDTO);

        // Assert
        assertNotNull(result);
        assertEquals("LOCATION", result.getChangeType());
        assertEquals("location", result.getFieldChanged());
        assertEquals("DC-1-Floor-1", result.getOldValue());
        assertEquals("DC-1-Floor-2", result.getNewValue());
        assertEquals("admin001", result.getChangedBy());
        assertEquals("PENDING", result.getApprovalStatus());
        verify(changeHistoryRepository, times(1)).save(any(AssetChangeHistory.class));
    }

    @Test
    void testRecordInventoryResult_MissingStatus_CreatesChange() {
        // Arrange
        when(inventoryRepository.save(any(AssetInventory.class)))
            .thenAnswer(invocation -> {
                AssetInventory inv = invocation.getArgument(0);
                inv.setId(1L);
                return inv;
            });
        when(changeHistoryRepository.save(any(AssetChangeHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AssetInventory result = inventoryService.recordInventoryResult(
            1L, "INV-20240529-0001", 100L, "Server 01", "SRV-001", "MISSING", "DC-2"
        );

        // Assert
        assertNotNull(result);
        assertEquals("MISSING", result.getStatus());
        verify(inventoryRepository, times(1)).save(any(AssetInventory.class));
        // Should also create change history for MISSING status
        verify(changeHistoryRepository, times(1)).save(any(AssetChangeHistory.class));
    }

    @Test
    void testRecordInventoryResult_VerifiedStatus_NoChange() {
        // Arrange
        when(inventoryRepository.save(any(AssetInventory.class)))
            .thenAnswer(invocation -> {
                AssetInventory inv = invocation.getArgument(0);
                inv.setId(1L);
                return inv;
            });

        // Act
        AssetInventory result = inventoryService.recordInventoryResult(
            1L, "INV-20240529-0001", 100L, "Server 01", "SRV-001", "VERIFIED", "DC-1"
        );

        // Assert
        assertNotNull(result);
        assertEquals("VERIFIED", result.getStatus());
        verify(inventoryRepository, times(1)).save(any(AssetInventory.class));
        // Should NOT create change history for VERIFIED status
        verify(changeHistoryRepository, never()).save(any(AssetChangeHistory.class));
    }

    @Test
    void testGetDiscrepancySummary() {
        // Arrange
        java.util.List<AssetInventory> records = java.util.Arrays.asList(
            AssetInventory.builder().status("VERIFIED").build(),
            AssetInventory.builder().status("VERIFIED").build(),
            AssetInventory.builder().status("MISSING").build(),
            AssetInventory.builder().status("CHANGED").build(),
            AssetInventory.builder().status("NEW").build()
        );

        when(inventoryRepository.list(any())).thenReturn(records);

        // Act
        Map<String, Integer> summary = inventoryService.getDiscrepancySummary(1L);

        // Assert
        assertNotNull(summary);
        assertEquals(2, summary.get("VERIFIED"));
        assertEquals(1, summary.get("MISSING"));
        assertEquals(1, summary.get("CHANGED"));
        assertEquals(1, summary.get("NEW"));
        assertEquals(0, summary.get("UNKNOWN"));
    }

    @Test
    void testGetPendingTasks() {
        // Arrange
        java.util.List<InventoryTask> tasks = java.util.Arrays.asList(
            InventoryTask.builder().id(1L).taskNumber("INV-001").status("PENDING").build(),
            InventoryTask.builder().id(2L).taskNumber("INV-002").status("PENDING").build()
        );

        when(taskRepository.list(any())).thenReturn(tasks);

        // Act
        java.util.List<InventoryTask> result = inventoryService.getPendingTasks();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}