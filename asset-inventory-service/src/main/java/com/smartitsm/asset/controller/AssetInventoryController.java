package com.smartitsm.asset.controller;

import com.smartitsm.asset.dto.AssetChangeHistoryDTO;
import com.smartitsm.asset.dto.AssetUsageStatsDTO;
import com.smartitsm.asset.dto.InventoryTaskCreateDTO;
import com.smartitsm.asset.entity.AssetChangeHistory;
import com.smartitsm.asset.entity.AssetInventory;
import com.smartitsm.asset.entity.InventoryTask;
import com.smartitsm.asset.service.AssetInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for asset inventory management.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class AssetInventoryController {

    private final AssetInventoryService inventoryService;

    // ========== Inventory Task Endpoints ==========

    /**
     * Create a new inventory task.
     */
    @PostMapping("/tasks")
    public InventoryTask createTask(@RequestBody InventoryTaskCreateDTO dto) {
        return inventoryService.createInventoryTask(dto);
    }

    /**
     * Get inventory task by ID.
     */
    @GetMapping("/tasks/{taskId}")
    public InventoryTask getTask(@PathVariable Long taskId) {
        return inventoryService.getTaskById(taskId);
    }

    /**
     * Get inventory task by task number.
     */
    @GetMapping("/tasks/by-number/{taskNumber}")
    public InventoryTask getTaskByNumber(@PathVariable String taskNumber) {
        return inventoryService.getTaskByNumber(taskNumber);
    }

    /**
     * Start an inventory task.
     */
    @PostMapping("/tasks/{taskId}/start")
    public InventoryTask startTask(@PathVariable Long taskId) {
        return inventoryService.startTask(taskId);
    }

    /**
     * Complete an inventory task.
     */
    @PostMapping("/tasks/{taskId}/complete")
    public InventoryTask completeTask(@PathVariable Long taskId, 
                                      @RequestParam(required = false) String completionNotes) {
        return inventoryService.completeTask(taskId, completionNotes);
    }

    /**
     * Get pending inventory tasks.
     */
    @GetMapping("/tasks/pending")
    public List<InventoryTask> getPendingTasks() {
        return inventoryService.getPendingTasks();
    }

    /**
     * Search inventory tasks.
     */
    @GetMapping("/tasks/search")
    public List<InventoryTask> searchTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.searchTasks(status, taskType, assignedTo, page, size);
    }

    // ========== Inventory Results Endpoints ==========

    /**
     * Record inventory result for an asset.
     */
    @PostMapping("/results")
    public AssetInventory recordResult(
            @RequestParam Long taskId,
            @RequestParam String taskNumber,
            @RequestParam Long assetId,
            @RequestParam String assetName,
            @RequestParam(required = false) String assetTag,
            @RequestParam String status,
            @RequestParam(required = false) String location) {
        return inventoryService.recordInventoryResult(taskId, taskNumber, assetId, assetName, assetTag, status, location);
    }

    /**
     * Get inventory records for a task.
     */
    @GetMapping("/tasks/{taskId}/results")
    public List<AssetInventory> getTaskResults(@PathVariable Long taskId) {
        return inventoryService.getInventoryRecords(taskId);
    }

    /**
     * Get discrepancy summary for a task.
     */
    @GetMapping("/tasks/{taskId}/discrepancies")
    public Map<String, Integer> getDiscrepancies(@PathVariable Long taskId) {
        return inventoryService.getDiscrepancySummary(taskId);
    }

    // ========== Asset Usage Statistics Endpoints ==========

    /**
     * Get asset usage statistics.
     */
    @GetMapping("/stats/usage")
    public AssetUsageStatsDTO getUsageStats(@RequestParam(required = false) String periodType) {
        return inventoryService.getAssetUsageStats(periodType);
    }

    // ========== Asset Change History Endpoints ==========

    /**
     * Record an asset change.
     */
    @PostMapping("/changes")
    public AssetChangeHistory recordChange(@RequestBody AssetChangeHistoryDTO dto) {
        return inventoryService.recordAssetChange(dto);
    }

    /**
     * Get change history for a specific asset.
     */
    @GetMapping("/changes/asset/{assetId}")
    public List<AssetChangeHistory> getAssetChanges(
            @PathVariable Long assetId,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.getAssetChangeHistory(assetId, changeType, page, size);
    }

    /**
     * Get all change history with filters.
     */
    @GetMapping("/changes")
    public List<AssetChangeHistory> getChangeHistory(
            @RequestParam(required = false) String assetCategory,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.getChangeHistory(assetCategory, changeType, 
            startDate != null ? startDate.toString() : null,
            endDate != null ? endDate.toString() : null, page, size);
    }
}