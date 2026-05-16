package com.smartitsm.asset.controller;

import com.smartitsm.asset.dto.*;
import com.smartitsm.asset.entity.Asset;
import com.smartitsm.asset.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Asset Controller - REST API for IT asset management.
 */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);
    private final AssetService assetService;

    /**
     * Create a new IT asset.
     */
    @PostMapping
    public ResponseEntity<Asset> createAsset(@Valid @RequestBody AssetCreateDTO dto) {
        log.info("POST /api/v1/assets - creating asset: {}", dto.getName());
        Asset asset = assetService.createAsset(dto);
        return ResponseEntity.ok(asset);
    }

    /**
     * Update an existing asset.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long id,
                                              @RequestBody AssetUpdateDTO dto) {
        log.info("PUT /api/v1/assets/{}", id);
        Asset asset = assetService.updateAsset(id, dto);
        return ResponseEntity.ok(asset);
    }

    /**
     * Get asset by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAsset(@PathVariable Long id) {
        log.info("GET /api/v1/assets/{}", id);
        Asset asset = assetService.getAsset(id);
        return ResponseEntity.ok(asset);
    }

    /**
     * Get asset by asset number.
     */
    @GetMapping("/number/{assetNumber}")
    public ResponseEntity<Asset> getAssetByNumber(@PathVariable String assetNumber) {
        log.info("GET /api/v1/assets/number/{}", assetNumber);
        Asset asset = assetService.getAssetByNumber(assetNumber);
        return ResponseEntity.ok(asset);
    }

    /**
     * Delete (retire) an asset.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        log.info("DELETE /api/v1/assets/{}", id);
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search assets with filters.
     */
    @GetMapping
    public ResponseEntity<?> searchAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/assets - keyword: {}, type: {}, status: {}", keyword, assetType, status);
        return ResponseEntity.ok(assetService.searchAssets(keyword, assetType, status, healthStatus, page, size));
    }

    /**
     * Get assets by type.
     */
    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<Asset>> getAssetsByType(@PathVariable String assetType) {
        log.info("GET /api/v1/assets/type/{}", assetType);
        return ResponseEntity.ok(assetService.getAssetsByType(assetType));
    }

    /**
     * Get assets by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Asset>> getAssetsByStatus(@PathVariable String status) {
        log.info("GET /api/v1/assets/status/{}", status);
        return ResponseEntity.ok(assetService.getAssetsByStatus(status));
    }

    /**
     * Get assets by assigned group.
     */
    @GetMapping("/group/{assignedGroup}")
    public ResponseEntity<List<Asset>> getAssetsByGroup(@PathVariable String assignedGroup) {
        log.info("GET /api/v1/assets/group/{}", assignedGroup);
        return ResponseEntity.ok(assetService.getAssetsByGroup(assignedGroup));
    }

    /**
     * Get assets at risk.
     */
    @GetMapping("/at-risk")
    public ResponseEntity<List<Asset>> getAssetsAtRisk() {
        log.info("GET /api/v1/assets/at-risk");
        return ResponseEntity.ok(assetService.getAssetsAtRisk());
    }

    /**
     * Get assets needing maintenance.
     */
    @GetMapping("/maintenance")
    public ResponseEntity<List<Asset>> getAssetsNeedingMaintenance() {
        log.info("GET /api/v1/assets/maintenance");
        return ResponseEntity.ok(assetService.getAssetsNeedingMaintenance());
    }

    /**
     * Get child assets in hierarchy.
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<Asset>> getChildAssets(@PathVariable Long id) {
        log.info("GET /api/v1/assets/{}/children", id);
        return ResponseEntity.ok(assetService.getChildAssets(id));
    }

    /**
     * Update monitoring metrics for an asset.
     */
    @PutMapping("/{id}/metrics")
    public ResponseEntity<Asset> updateMonitoringMetrics(
            @PathVariable Long id,
            @RequestParam(required = false) Double cpuUsage,
            @RequestParam(required = false) Double memoryUsage,
            @RequestParam(required = false) Double diskUsage,
            @RequestParam(required = false) Double networkLatency,
            @RequestParam(required = false) Double errorRate) {
        log.info("PUT /api/v1/assets/{}/metrics", id);
        Asset asset = assetService.updateMonitoringMetrics(id, cpuUsage, memoryUsage, diskUsage, networkLatency, errorRate);
        return ResponseEntity.ok(asset);
    }

    /**
     * Analyze asset health with AI.
     */
    @GetMapping("/{id}/health")
    public ResponseEntity<AssetHealthDTO> analyzeAssetHealth(@PathVariable Long id) {
        log.info("GET /api/v1/assets/{}/health", id);
        AssetHealthDTO health = assetService.analyzeAssetHealth(id);
        return ResponseEntity.ok(health);
    }

    /**
     * Get asset statistics dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<AssetService.AssetStats> getAssetStats() {
        log.info("GET /api/v1/assets/stats");
        return ResponseEntity.ok(assetService.getAssetStats());
    }
}
