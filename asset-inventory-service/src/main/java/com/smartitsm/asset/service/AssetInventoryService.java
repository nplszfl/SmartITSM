package com.smartitsm.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.asset.dto.AssetChangeHistoryDTO;
import com.smartitsm.asset.dto.AssetUsageStatsDTO;
import com.smartitsm.asset.dto.InventoryTaskCreateDTO;
import com.smartitsm.asset.entity.AssetChangeHistory;
import com.smartitsm.asset.entity.AssetInventory;
import com.smartitsm.asset.entity.InventoryTask;
import com.smartitsm.asset.repository.AssetChangeHistoryRepository;
import com.smartitsm.asset.repository.AssetInventoryRepository;
import com.smartitsm.asset.repository.InventoryTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Asset Inventory Service - handles inventory tasks, usage statistics, and change tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInventoryService {

    private final InventoryTaskRepository taskRepository;
    private final AssetInventoryRepository inventoryRepository;
    private final AssetChangeHistoryRepository changeHistoryRepository;

    // Counter for task numbering
    private final AtomicLong taskSequence = new AtomicLong(System.currentTimeMillis() % 10000);

    /**
     * Create a new inventory task.
     */
    @Transactional
    public InventoryTask createInventoryTask(InventoryTaskCreateDTO dto) {
        log.info("Creating inventory task: {}", dto.getTaskType());

        String taskNumber = generateTaskNumber();

        InventoryTask task = InventoryTask.builder()
            .taskNumber(taskNumber)
            .taskType(dto.getTaskType() != null ? dto.getTaskType() : "FULL")
            .status("PENDING")
            .scheduleType(dto.getScheduleType() != null ? dto.getScheduleType() : "ONE_TIME")
            .scheduledAt(dto.getScheduledAt() != null ? dto.getScheduledAt() : LocalDateTime.now())
            .assetCategory(dto.getAssetCategory())
            .assetLocation(dto.getAssetLocation())
            .targetAssetCount(dto.getTargetAssetCount())
            .assignedTo(dto.getAssignedTo())
            .assignedGroup(dto.getAssignedGroup())
            .notes(dto.getNotes())
            .triggerSource("MANUAL")
            .build();

        taskRepository.save(task);
        log.info("Created inventory task {} with number {}", task.getId(), taskNumber);

        return task;
    }

    /**
     * Get inventory task by ID.
     */
    public InventoryTask getTaskById(Long taskId) {
        return taskRepository.getById(taskId);
    }

    /**
     * Get inventory task by task number.
     */
    public InventoryTask getTaskByNumber(String taskNumber) {
        return taskRepository.getOne(
            new QueryWrapper<InventoryTask>().eq("task_number", taskNumber)
        );
    }

    /**
     * Start an inventory task.
     */
    @Transactional
    public InventoryTask startTask(Long taskId) {
        InventoryTask task = taskRepository.getById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        task.setStatus("IN_PROGRESS");
        task.setStartedAt(LocalDateTime.now());
        taskRepository.updateById(task);

        log.info("Started inventory task {} - {}", task.getTaskNumber(), task.getTaskType());
        return task;
    }

    /**
     * Complete an inventory task.
     */
    @Transactional
    public InventoryTask completeTask(Long taskId, String completionNotes) {
        InventoryTask task = taskRepository.getById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletionNotes(completionNotes);

        // Calculate results from inventory records
        List<AssetInventory> records = inventoryRepository.list(
            new QueryWrapper<AssetInventory>().eq("task_id", taskId)
        );

        task.setTotalAssetsFound((int) records.size());
        task.setTotalAssetsMissing((int) records.stream().filter(r -> "MISSING".equals(r.getStatus())).count());
        task.setTotalAssetsChanged((int) records.stream().filter(r -> "CHANGED".equals(r.getStatus())).count());
        task.setTotalDiscrepancies((int) records.stream().filter(r -> r.getDiscrepancyType() != null).count());
        task.setActualAssetCount(records.size());

        taskRepository.updateById(task);

        log.info("Completed inventory task {} - Found: {}, Missing: {}, Changed: {}", 
            task.getTaskNumber(), task.getTotalAssetsFound(), task.getTotalAssetsMissing(), task.getTotalAssetsChanged());
        return task;
    }

    /**
     * Get asset usage statistics.
     */
    public AssetUsageStatsDTO getAssetUsageStats(String periodType) {
        // Simulated statistics - in production would query actual asset data
        return AssetUsageStatsDTO.builder()
            .totalAssets(500L)
            .activeAssets(450L)
            .inactiveAssets(30L)
            .retiredAssets(20L)
            .overallUtilizationRate(78.5)
            .serversCount(100L)
            .serversUtilization(85.0)
            .workstationsCount(300L)
            .workstationsUtilization(75.0)
            .networkDevicesCount(50L)
            .networkUtilization(80.0)
            .storageCount(50L)
            .storageUtilization(70.0)
            .utilizationTrend(2.5)
            .trendPeriod(periodType != null ? periodType : "WEEKLY")
            .reportDate(LocalDateTime.now())
            .periodStart(LocalDateTime.now().minusDays(7).toLocalDate().toString())
            .periodEnd(LocalDateTime.now().toLocalDate().toString())
            .build();
    }

    /**
     * Record an asset change.
     */
    @Transactional
    public AssetChangeHistory recordAssetChange(AssetChangeHistoryDTO dto) {
        log.info("Recording asset change for {} - field: {}", dto.getAssetName(), dto.getFieldChanged());

        AssetChangeHistory history = AssetChangeHistory.builder()
            .assetId(dto.getAssetId())
            .assetName(dto.getAssetName())
            .assetTag(dto.getAssetTag())
            .assetCategory(dto.getAssetCategory())
            .changeType(dto.getChangeType())
            .fieldChanged(dto.getFieldChanged())
            .oldValue(dto.getOldValue())
            .newValue(dto.getNewValue())
            .changedAt(LocalDateTime.now())
            .changedBy(dto.getChangedBy())
            .changedByName(dto.getChangedByName())
            .changeReason(dto.getChangeReason())
            .source(dto.getSource() != null ? dto.getSource() : "MANUAL")
            .sourceId(dto.getSourceId())
            .impactLevel(dto.getImpactLevel() != null ? dto.getImpactLevel() : "LOW")
            .approvalStatus("PENDING")
            .build();

        changeHistoryRepository.save(history);
        
        log.info("Recorded change for asset {} - {} changed from '{}' to '{}'", 
            dto.getAssetName(), dto.getFieldChanged(), dto.getOldValue(), dto.getNewValue());
        
        return history;
    }

    /**
     * Get change history for an asset.
     */
    public List<AssetChangeHistory> getAssetChangeHistory(Long assetId, String changeType, int page, int size) {
        QueryWrapper<AssetChangeHistory> query = new QueryWrapper<>();
        query.eq("asset_id", assetId);
        if (changeType != null) {
            query.eq("change_type", changeType);
        }
        query.orderByDesc("changed_at");

        Page<AssetChangeHistory> pageResult = changeHistoryRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Get all change history with filters.
     */
    public List<AssetChangeHistory> getChangeHistory(String assetCategory, String changeType, 
                                                    String startDate, String endDate, int page, int size) {
        QueryWrapper<AssetChangeHistory> query = new QueryWrapper<>();
        
        if (assetCategory != null) query.eq("asset_category", assetCategory);
        if (changeType != null) query.eq("change_type", changeType);
        if (startDate != null) query.ge("changed_at", startDate);
        if (endDate != null) query.le("changed_at", endDate);
        
        query.orderByDesc("changed_at");

        Page<AssetChangeHistory> pageResult = changeHistoryRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Get pending inventory tasks.
     */
    public List<InventoryTask> getPendingTasks() {
        return taskRepository.list(
            new QueryWrapper<InventoryTask>()
                .eq("status", "PENDING")
                .orderByAsc("scheduled_at")
        );
    }

    /**
     * Get tasks by schedule type.
     */
    public List<InventoryTask> getTasksBySchedule(String scheduleType) {
        return taskRepository.list(
            new QueryWrapper<InventoryTask>()
                .eq("schedule_type", scheduleType)
                .orderByDesc("created_at")
        );
    }

    /**
     * Search inventory tasks.
     */
    public List<InventoryTask> searchTasks(String status, String taskType, String assignedTo, int page, int size) {
        QueryWrapper<InventoryTask> query = new QueryWrapper<>();
        
        if (status != null) query.eq("status", status);
        if (taskType != null) query.eq("task_type", taskType);
        if (assignedTo != null) query.eq("assigned_to", assignedTo);
        
        query.orderByDesc("created_at");

        Page<InventoryTask> pageResult = taskRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Record inventory result for an asset.
     */
    @Transactional
    public AssetInventory recordInventoryResult(Long taskId, String taskNumber, Long assetId, 
                                                 String assetName, String assetTag, 
                                                 String currentStatus, String location) {
        AssetInventory inventory = AssetInventory.builder()
            .taskId(taskId)
            .taskNumber(taskNumber)
            .assetId(assetId)
            .assetName(assetName)
            .assetTag(assetTag)
            .status(currentStatus)
            .currentLocation(location)
            .verifiedAt(LocalDateTime.now())
            .build();

        inventoryRepository.save(inventory);
        
        // If status is MISSING or CHANGED, create change history
        if ("MISSING".equals(currentStatus) || "CHANGED".equals(currentStatus)) {
            createChangeFromInventory(inventory);
        }
        
        return inventory;
    }

    /**
     * Get inventory records for a task.
     */
    public List<AssetInventory> getInventoryRecords(Long taskId) {
        return inventoryRepository.list(
            new QueryWrapper<AssetInventory>()
                .eq("task_id", taskId)
                .orderByDesc("verified_at")
        );
    }

    /**
     * Get discrepancy summary for a task.
     */
    public Map<String, Integer> getDiscrepancySummary(Long taskId) {
        List<AssetInventory> records = inventoryRepository.list(
            new QueryWrapper<AssetInventory>().eq("task_id", taskId)
        );

        Map<String, Integer> summary = new HashMap<>();
        summary.put("VERIFIED", (int) records.stream().filter(r -> "VERIFIED".equals(r.getStatus())).count());
        summary.put("MISSING", (int) records.stream().filter(r -> "MISSING".equals(r.getStatus())).count());
        summary.put("CHANGED", (int) records.stream().filter(r -> "CHANGED".equals(r.getStatus())).count());
        summary.put("NEW", (int) records.stream().filter(r -> "NEW".equals(r.getStatus())).count());
        summary.put("UNKNOWN", (int) records.stream().filter(r -> "UNKNOWN".equals(r.getStatus())).count());

        return summary;
    }

    /**
     * Scheduled task to generate recurring inventory tasks.
     */
    @Scheduled(cron = "0 0 2 * * ?")  // Run at 2 AM daily
    public void generateRecurringTasks() {
        log.info("Generating recurring inventory tasks...");

        // Find all schedule types that need tasks today
        List<String> scheduleTypes = Arrays.asList("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY");
        
        for (String scheduleType : scheduleTypes) {
            List<InventoryTask> tasks = taskRepository.list(
                new QueryWrapper<InventoryTask>()
                    .eq("schedule_type", scheduleType)
                    .eq("status", "COMPLETED")
            );

            for (InventoryTask task : tasks) {
                if (shouldCreateNewTask(task)) {
                    InventoryTaskCreateDTO dto = InventoryTaskCreateDTO.builder()
                        .taskType(task.getTaskType())
                        .scheduleType(task.getScheduleType())
                        .scheduledAt(calculateNextSchedule(task.getScheduleType()))
                        .assetCategory(task.getAssetCategory())
                        .assetLocation(task.getAssetLocation())
                        .assignedTo(task.getAssignedTo())
                        .assignedGroup(task.getAssignedGroup())
                        .notes("Auto-generated from previous task: " + task.getTaskNumber())
                        .build();

                    createInventoryTask(dto);
                    log.info("Created recurring task for {} schedule", scheduleType);
                }
            }
        }
    }

    // Helper methods

    private String generateTaskNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = taskSequence.incrementAndGet();
        return String.format("INV-%s-%04d", date, seq % 10000);
    }

    private boolean shouldCreateNewTask(InventoryTask previousTask) {
        if (previousTask.getScheduledAt() == null) return false;
        
        LocalDateTime nextSchedule = calculateNextSchedule(previousTask.getScheduleType());
        return nextSchedule != null && nextSchedule.isBefore(LocalDateTime.now());
    }

    private LocalDateTime calculateNextSchedule(String scheduleType) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (scheduleType) {
            case "DAILY" -> now.plusDays(1).withHour(2).withMinute(0);
            case "WEEKLY" -> now.plusWeeks(1).withHour(2).withMinute(0);
            case "MONTHLY" -> now.plusMonths(1).withHour(2).withMinute(0);
            case "QUARTERLY" -> now.plusMonths(3).withHour(2).withMinute(0);
            case "YEARLY" -> now.plusYears(1).withHour(2).withMinute(0);
            default -> null;
        };
    }

    private void createChangeFromInventory(AssetInventory inventory) {
        AssetChangeHistoryDTO dto = AssetChangeHistoryDTO.builder()
            .assetId(inventory.getAssetId())
            .assetName(inventory.getAssetName())
            .assetTag(inventory.getAssetTag())
            .changeType("INVENTORY_CHECK")
            .fieldChanged("status")
            .oldValue("VERIFIED")
            .newValue(inventory.getStatus())
            .source("INVENTORY")
            .sourceId(inventory.getTaskNumber())
            .impactLevel("MEDIUM")
            .build();

        recordAssetChange(dto);
    }
}