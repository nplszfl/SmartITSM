package com.smartitsm.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.asset.dto.*;
import com.smartitsm.asset.entity.Asset;
import com.smartitsm.asset.repository.AssetRepository;
import com.smartitsm.common.ai.AIClient;
import com.smartitsm.common.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Asset Service - core business logic for IT asset management with AI-powered health analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AIClient aiClient;

    private final AtomicLong assetSequence = new AtomicLong(System.currentTimeMillis() % 10000);

    /**
     * Create a new IT asset with AI health scoring.
     */
    @Transactional
    public Asset createAsset(AssetCreateDTO dto) {
        log.info("Creating asset: {} of type {}", dto.getName(), dto.getAssetType());

        String assetNumber = generateAssetNumber();

        Asset asset = Asset.builder()
                .assetNumber(assetNumber)
                .name(dto.getName())
                .description(dto.getDescription())
                .assetType(dto.getAssetType())
                .subType(dto.getSubType())
                .manufacturer(dto.getManufacturer())
                .model(dto.getModel())
                .serialNumber(dto.getSerialNumber())
                .status("ACTIVE")
                .healthStatus("UNKNOWN")
                .location(dto.getLocation())
                .department(dto.getDepartment())
                .assignedTo(dto.getAssignedTo())
                .assignedGroup(dto.getAssignedGroup())
                .ipAddress(dto.getIpAddress())
                .macAddress(dto.getMacAddress())
                .hostname(dto.getHostname())
                .operatingSystem(dto.getOperatingSystem())
                .cpuCores(dto.getCpuCores())
                .memoryGB(dto.getMemoryGB())
                .storageGB(dto.getStorageGB())
                .purchaseDate(dto.getPurchaseDate())
                .warrantyExpiry(dto.getWarrantyExpiry())
                .installationDate(dto.getInstallationDate())
                .expectedLifeYears(dto.getExpectedLifeYears())
                .purchaseCost(dto.getPurchaseCost())
                .maintenanceCost(dto.getMaintenanceCost())
                .replacementCost(dto.getReplacementCost())
                .parentAssetId(dto.getParentAssetId())
                .vendor(dto.getVendor())
                .supportLevel(dto.getSupportLevel())
                .tags(dto.getTags())
                .build();

        // Set warranty status
        updateWarrantyStatus(asset);

        // Apply initial AI health scoring
        applyAIHealthScoring(asset);

        assetRepository.insert(asset);
        log.info("Created asset {} with health score {}", assetNumber, asset.getHealthScore());

        return asset;
    }

    /**
     * Update an existing asset.
     */
    @Transactional
    public Asset updateAsset(Long assetId, AssetUpdateDTO dto) {
        Asset asset = assetRepository.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetId);
        }

        // Update basic fields
        if (dto.getName() != null) asset.setName(dto.getName());
        if (dto.getDescription() != null) asset.setDescription(dto.getDescription());
        if (dto.getAssetType() != null) asset.setAssetType(dto.getAssetType());
        if (dto.getSubType() != null) asset.setSubType(dto.getSubType());
        if (dto.getManufacturer() != null) asset.setManufacturer(dto.getManufacturer());
        if (dto.getModel() != null) asset.setModel(dto.getModel());
        if (dto.getSerialNumber() != null) asset.setSerialNumber(dto.getSerialNumber());
        if (dto.getStatus() != null) asset.setStatus(dto.getStatus());
        if (dto.getHealthStatus() != null) asset.setHealthStatus(dto.getHealthStatus());
        if (dto.getLocation() != null) asset.setLocation(dto.getLocation());
        if (dto.getDepartment() != null) asset.setDepartment(dto.getDepartment());
        if (dto.getAssignedTo() != null) asset.setAssignedTo(dto.getAssignedTo());
        if (dto.getAssignedGroup() != null) asset.setAssignedGroup(dto.getAssignedGroup());
        if (dto.getIpAddress() != null) asset.setIpAddress(dto.getIpAddress());
        if (dto.getMacAddress() != null) asset.setMacAddress(dto.getMacAddress());
        if (dto.getHostname() != null) asset.setHostname(dto.getHostname());
        if (dto.getOperatingSystem() != null) asset.setOperatingSystem(dto.getOperatingSystem());
        if (dto.getCpuCores() != null) asset.setCpuCores(dto.getCpuCores());
        if (dto.getMemoryGB() != null) asset.setMemoryGB(dto.getMemoryGB());
        if (dto.getStorageGB() != null) asset.setStorageGB(dto.getStorageGB());

        // Monitoring metrics
        if (dto.getCpuUsage() != null) asset.setCpuUsage(dto.getCpuUsage());
        if (dto.getMemoryUsage() != null) asset.setMemoryUsage(dto.getMemoryUsage());
        if (dto.getDiskUsage() != null) asset.setDiskUsage(dto.getDiskUsage());
        if (dto.getNetworkLatency() != null) asset.setNetworkLatency(dto.getNetworkLatency());
        if (dto.getErrorRate() != null) asset.setErrorRate(dto.getErrorRate());

        // Lifecycle
        if (dto.getWarrantyExpiry() != null) asset.setWarrantyExpiry(dto.getWarrantyExpiry());
        if (dto.getRetirementDate() != null) asset.setRetirementDate(dto.getRetirementDate());

        // Financial
        if (dto.getMaintenanceCost() != null) asset.setMaintenanceCost(dto.getMaintenanceCost());

        // Maintenance
        if (dto.getLastMaintenanceDate() != null) asset.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        if (dto.getNextMaintenanceDate() != null) asset.setNextMaintenanceDate(dto.getNextMaintenanceDate());
        if (dto.getMaintenanceIntervalDays() != null) asset.setMaintenanceIntervalDays(dto.getMaintenanceIntervalDays());

        // Relationships
        if (dto.getParentAssetId() != null) asset.setParentAssetId(dto.getParentAssetId());
        if (dto.getRelatedTicketIds() != null) asset.setRelatedTicketIds(dto.getRelatedTicketIds());
        if (dto.getConfigurationItems() != null) asset.setConfigurationItems(dto.getConfigurationItems());

        // Metadata
        if (dto.getVendor() != null) asset.setVendor(dto.getVendor());
        if (dto.getVendorContact() != null) asset.setVendorContact(dto.getVendorContact());
        if (dto.getSupportLevel() != null) asset.setSupportLevel(dto.getSupportLevel());
        if (dto.getTags() != null) asset.setTags(dto.getTags());

        // Update warranty status
        updateWarrantyStatus(asset);

        // Re-score health if metrics changed
        if (dto.getCpuUsage() != null || dto.getMemoryUsage() != null || dto.getDiskUsage() != null) {
            applyAIHealthScoring(asset);
        }

        assetRepository.updateById(asset);
        log.info("Updated asset {}", asset.getAssetNumber());

        return asset;
    }

    /**
     * Get asset by ID.
     */
    public Asset getAsset(Long assetId) {
        Asset asset = assetRepository.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetId);
        }
        return asset;
    }

    /**
     * Get asset by asset number.
     */
    public Asset getAssetByNumber(String assetNumber) {
        Asset asset = assetRepository.findByAssetNumber(assetNumber);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetNumber);
        }
        return asset;
    }

    /**
     * Delete (soft delete) an asset.
     */
    @Transactional
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetId);
        }
        asset.setStatus("RETIRED");
        assetRepository.updateById(asset);
        log.info("Retired asset {}", asset.getAssetNumber());
    }

    /**
     * Search assets with pagination.
     */
    public IPage<Asset> searchAssets(String keyword, String assetType, String status, 
                                     String healthStatus, int page, int size) {
        QueryWrapper<Asset> query = new QueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("name", keyword)
                    .or().like("description", keyword)
                    .or().like("asset_number", keyword)
                    .or().like("serial_number", keyword));
        }
        if (assetType != null) query.eq("asset_type", assetType);
        if (status != null) query.eq("status", status);
        if (healthStatus != null) query.eq("health_status", healthStatus);

        query.orderByDesc("updated_at");

        return assetRepository.selectPage(new Page<>(page, size), query);
    }

    /**
     * Get assets by type.
     */
    public List<Asset> getAssetsByType(String assetType) {
        return assetRepository.findByAssetType(assetType);
    }

    /**
     * Get assets by status.
     */
    public List<Asset> getAssetsByStatus(String status) {
        return assetRepository.findByStatus(status);
    }

    /**
     * Get assets by assigned group.
     */
    public List<Asset> getAssetsByGroup(String assignedGroup) {
        return assetRepository.findByAssignedGroup(assignedGroup);
    }

    /**
     * Get assets at risk (high failure probability).
     */
    public List<Asset> getAssetsAtRisk() {
        QueryWrapper<Asset> query = new QueryWrapper<>();
        query.in("risk_level", List.of("CRITICAL", "HIGH"))
                .orderByDesc("failure_probability");
        return assetRepository.selectList(query);
    }

    /**
     * Get assets needing maintenance.
     */
    public List<Asset> getAssetsNeedingMaintenance() {
        LocalDate today = LocalDate.now();
        QueryWrapper<Asset> query = new QueryWrapper<>();
        query.le("next_maintenance_date", today.plusDays(7))
                .eq("status", "ACTIVE")
                .orderByAsc("next_maintenance_date");
        return assetRepository.selectList(query);
    }

    /**
     * Get child assets in hierarchy.
     */
    public List<Asset> getChildAssets(Long parentAssetId) {
        return assetRepository.findByParentAssetId(parentAssetId);
    }

    /**
     * Update asset monitoring metrics.
     */
    @Transactional
    public Asset updateMonitoringMetrics(Long assetId, Double cpuUsage, Double memoryUsage,
                                         Double diskUsage, Double networkLatency, Double errorRate) {
        Asset asset = assetRepository.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetId);
        }

        asset.setCpuUsage(cpuUsage);
        asset.setMemoryUsage(memoryUsage);
        asset.setDiskUsage(diskUsage);
        asset.setNetworkLatency(networkLatency);
        asset.setErrorRate(errorRate);
        asset.setLastMonitoring(LocalDateTime.now());

        // Re-analyze health with new metrics
        applyAIHealthScoring(asset);

        assetRepository.updateById(asset);
        log.info("Updated monitoring metrics for asset {}", asset.getAssetNumber());

        return asset;
    }

    /**
     * Perform AI-powered health analysis on an asset.
     */
    public AssetHealthDTO analyzeAssetHealth(Long assetId) {
        Asset asset = assetRepository.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("ASSET_NOT_FOUND", "Asset not found: " + assetId);
        }

        return performAIHealthAnalysis(asset);
    }

    /**
     * Get asset statistics dashboard.
     */
    public AssetStats getAssetStats() {
        AssetStats stats = new AssetStats();
        stats.setTotal(assetRepository.selectCount(null));
        stats.setActive(assetRepository.countByStatus("ACTIVE"));
        stats.setInMaintenance(assetRepository.countByStatus("MAINTENANCE"));
        stats.setRetired(assetRepository.countByStatus("RETIRED"));
        stats.setHealthy(assetRepository.selectList(
                new QueryWrapper<Asset>().eq("health_status", "HEALTHY")).size());
        stats.setWarning(assetRepository.selectList(
                new QueryWrapper<Asset>().eq("health_status", "WARNING")).size());
        stats.setCritical(assetRepository.selectList(
                new QueryWrapper<Asset>().eq("health_status", "CRITICAL")).size());

        // Count by type
        stats.setServerCount(assetRepository.countByAssetType("SERVER"));
        stats.setNetworkCount(assetRepository.countByAssetType("NETWORK"));
        stats.setStorageCount(assetRepository.countByAssetType("STORAGE"));
        stats.setEndpointCount(assetRepository.countByAssetType("ENDPOINT"));
        stats.setCloudCount(assetRepository.countByAssetType("CLOUD"));
        stats.setDatabaseCount(assetRepository.countByAssetType("DATABASE"));

        // At-risk assets
        stats.setAtRiskCount(assetRepository.selectList(
                new QueryWrapper<Asset>().in("risk_level", List.of("CRITICAL", "HIGH"))).size());

        // Warranty expiring soon (30 days)
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        stats.setWarrantyExpiringSoonCount(assetRepository.selectList(
                new QueryWrapper<Asset>()
                        .le("warranty_expiry", thirtyDaysFromNow)
                        .gt("warranty_expiry", LocalDate.now())
                        .eq("status", "ACTIVE")).size());

        return stats;
    }

    /**
     * Apply AI health scoring to an asset.
     */
    private void applyAIHealthScoring(Asset asset) {
        try {
            int ageYears = 0;
            if (asset.getPurchaseDate() != null) {
                ageYears = (int) ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now());
            }

            AIClient.AssetHealthContext context = AIClient.AssetHealthContext.builder()
                    .assetName(asset.getName())
                    .assetType(asset.getAssetType())
                    .manufacturer(asset.getManufacturer())
                    .model(asset.getModel())
                    .ageYears(ageYears)
                    .purchaseDate(asset.getPurchaseDate())
                    .warrantyStatus(asset.getWarrantyStatus())
                    .cpuUsage(asset.getCpuUsage() != null ? asset.getCpuUsage() : 0.0)
                    .memoryUsage(asset.getMemoryUsage() != null ? asset.getMemoryUsage() : 0.0)
                    .diskUsage(asset.getDiskUsage() != null ? asset.getDiskUsage() : 0.0)
                    .networkLatency(asset.getNetworkLatency() != null ? asset.getNetworkLatency() : 0.0)
                    .errorRate(asset.getErrorRate() != null ? asset.getErrorRate() : 0.0)
                    .incidentCountLast30Days(0) // Would come from ticket service
                    .maintenanceCompliance(asset.getMaintenanceCompliance() != null ? asset.getMaintenanceCompliance() : 100.0)
                    .failureProbability(asset.getFailureProbability() != null ? asset.getFailureProbability() : 10.0)
                    .businessImpact(asset.getBusinessImpact() != null ? asset.getBusinessImpact() : "MEDIUM")
                    .build();

            AIClient.AssetHealthAnalysis analysis = aiClient.analyzeAssetHealth(context);

            if (analysis != null) {
                asset.setHealthScore(analysis.getHealthScore());
                asset.setRiskLevel(analysis.getRiskLevel());
                asset.setAiRecommendations(String.join("; ", analysis.getRecommendations()));
                asset.setHealthStatus(determineHealthStatus(analysis.getHealthScore()));
                asset.setFailureProbability(analysis.getHealthScore() > 50 ? 
                        100 - analysis.getHealthScore() : analysis.getHealthScore());
            }
        } catch (Exception e) {
            log.warn("AI health scoring failed for asset {}, using rule-based fallback: {}",
                    asset.getAssetNumber(), e.getMessage());
            // Rule-based fallback
            calculateRuleBasedHealth(asset);
        }
    }

    /**
     * Perform detailed AI health analysis and return DTO.
     */
    private AssetHealthDTO performAIHealthAnalysis(Asset asset) {
        int ageYears = 0;
        if (asset.getPurchaseDate() != null) {
            ageYears = (int) ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now());
        }

        AIClient.AssetHealthContext context = AIClient.AssetHealthContext.builder()
                .assetName(asset.getName())
                .assetType(asset.getAssetType())
                .manufacturer(asset.getManufacturer())
                .model(asset.getModel())
                .ageYears(ageYears)
                .purchaseDate(asset.getPurchaseDate())
                .warrantyStatus(asset.getWarrantyStatus())
                .cpuUsage(asset.getCpuUsage() != null ? asset.getCpuUsage() : 0.0)
                .memoryUsage(asset.getMemoryUsage() != null ? asset.getMemoryUsage() : 0.0)
                .diskUsage(asset.getDiskUsage() != null ? asset.getDiskUsage() : 0.0)
                .networkLatency(asset.getNetworkLatency() != null ? asset.getNetworkLatency() : 0.0)
                .errorRate(asset.getErrorRate() != null ? asset.getErrorRate() : 0.0)
                .incidentCountLast30Days(0)
                .maintenanceCompliance(asset.getMaintenanceCompliance() != null ? asset.getMaintenanceCompliance() : 100.0)
                .failureProbability(asset.getFailureProbability() != null ? asset.getFailureProbability() : 10.0)
                .businessImpact(asset.getBusinessImpact() != null ? asset.getBusinessImpact() : "MEDIUM")
                .build();

        try {
            AIClient.AssetHealthAnalysis analysis = aiClient.analyzeAssetHealth(context);

            if (analysis != null) {
                return AssetHealthDTO.builder()
                        .assetId(asset.getId())
                        .assetName(asset.getName())
                        .healthScore(analysis.getHealthScore())
                        .riskLevel(analysis.getRiskLevel())
                        .healthStatus(determineHealthStatus(analysis.getHealthScore()))
                        .prediction(analysis.getPrediction())
                        .failureProbability(analysis.getHealthScore() > 50 ? 100 - analysis.getHealthScore() : analysis.getHealthScore())
                        .recommendations(analysis.getRecommendations())
                        .riskFactors(extractRiskFactors(asset, analysis))
                        .analyzedAt(LocalDateTime.now().toString())
                        .confidence("HIGH")
                        .build();
            }
        } catch (Exception e) {
            log.warn("AI analysis failed for asset {}, using rule-based: {}", asset.getAssetNumber(), e.getMessage());
        }

        // Rule-based fallback
        calculateRuleBasedHealth(asset);
        return buildRuleBasedHealthDTO(asset);
    }

    /**
     * Rule-based health calculation fallback.
     */
    private void calculateRuleBasedHealth(Asset asset) {
        double healthScore = 70.0; // Base score

        // Age factor (older = lower score)
        if (asset.getPurchaseDate() != null) {
            int ageYears = (int) ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now());
            healthScore -= Math.min(ageYears * 3, 20); // Max 20 points reduction for age
        }

        // Warranty expired
        if ("EXPIRED".equals(asset.getWarrantyStatus())) {
            healthScore -= 10;
        }

        // High CPU usage
        if (asset.getCpuUsage() != null && asset.getCpuUsage() > 90) {
            healthScore -= 15;
        } else if (asset.getCpuUsage() != null && asset.getCpuUsage() > 80) {
            healthScore -= 5;
        }

        // High memory usage
        if (asset.getMemoryUsage() != null && asset.getMemoryUsage() > 90) {
            healthScore -= 10;
        } else if (asset.getMemoryUsage() != null && asset.getMemoryUsage() > 80) {
            healthScore -= 5;
        }

        // High disk usage
        if (asset.getDiskUsage() != null && asset.getDiskUsage() > 90) {
            healthScore -= 10;
        } else if (asset.getDiskUsage() != null && asset.getDiskUsage() > 85) {
            healthScore -= 5;
        }

        // High error rate
        if (asset.getErrorRate() != null && asset.getErrorRate() > 5) {
            healthScore -= 15;
        } else if (asset.getErrorRate() != null && asset.getErrorRate() > 1) {
            healthScore -= 5;
        }

        // High network latency
        if (asset.getNetworkLatency() != null && asset.getNetworkLatency() > 100) {
            healthScore -= 10;
        } else if (asset.getNetworkLatency() != null && asset.getNetworkLatency() > 50) {
            healthScore -= 5;
        }

        healthScore = Math.max(0, Math.min(100, healthScore));
        asset.setHealthScore(healthScore);
        asset.setHealthStatus(determineHealthStatus(healthScore));

        // Determine risk level
        if (healthScore < 30) {
            asset.setRiskLevel("CRITICAL");
        } else if (healthScore < 50) {
            asset.setRiskLevel("HIGH");
        } else if (healthScore < 70) {
            asset.setRiskLevel("MEDIUM");
        } else {
            asset.setRiskLevel("LOW");
        }

        asset.setFailureProbability(100 - healthScore);
    }

    private String determineHealthStatus(double healthScore) {
        if (healthScore >= 80) return "HEALTHY";
        if (healthScore >= 50) return "WARNING";
        return "CRITICAL";
    }

    private List<String> extractRiskFactors(Asset asset, AIClient.AssetHealthAnalysis analysis) {
        return analysis.getRecommendations().stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    private AssetHealthDTO buildRuleBasedHealthDTO(Asset asset) {
        return AssetHealthDTO.builder()
                .assetId(asset.getId())
                .assetName(asset.getName())
                .healthScore(asset.getHealthScore())
                .riskLevel(asset.getRiskLevel())
                .healthStatus(asset.getHealthStatus())
                .failureProbability(asset.getFailureProbability())
                .recommendations(generateRuleBasedRecommendations(asset))
                .analyzedAt(LocalDateTime.now().toString())
                .confidence("MEDIUM")
                .build();
    }

    private List<String> generateRuleBasedRecommendations(Asset asset) {
        List<String> recs = new java.util.ArrayList<>();
        if (asset.getCpuUsage() != null && asset.getCpuUsage() > 80) {
            recs.add("CPU usage is high (" + asset.getCpuUsage() + "%) - consider scaling or optimizing");
        }
        if (asset.getMemoryUsage() != null && asset.getMemoryUsage() > 80) {
            recs.add("Memory usage is high (" + asset.getMemoryUsage() + "%) - consider adding RAM");
        }
        if (asset.getDiskUsage() != null && asset.getDiskUsage() > 85) {
            recs.add("Disk space running low (" + asset.getDiskUsage() + "%) - plan storage expansion");
        }
        if ("EXPIRED".equals(asset.getWarrantyStatus())) {
            recs.add("Warranty expired - review maintenance contract");
        }
        if (asset.getPurchaseDate() != null) {
            int ageYears = (int) ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now());
            if (ageYears > 5) {
                recs.add("Asset is " + ageYears + " years old - consider replacement planning");
            }
        }
        if (recs.isEmpty()) {
            recs.add("Continue regular monitoring");
        }
        return recs;
    }

    private void updateWarrantyStatus(Asset asset) {
        if (asset.getWarrantyExpiry() == null) {
            asset.setWarrantyStatus("UNKNOWN");
            return;
        }
        if (asset.getWarrantyExpiry().isBefore(LocalDate.now())) {
            asset.setWarrantyStatus("EXPIRED");
        } else {
            asset.setWarrantyStatus("ACTIVE");
        }
    }

    private String generateAssetNumber() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long seq = assetSequence.incrementAndGet();
        return String.format("AST-%s-%05d", year, seq % 100000);
    }

    @Data
    public static class AssetStats {
        private long total;
        private long active;
        private long inMaintenance;
        private long retired;
        private long healthy;
        private long warning;
        private long critical;
        private long serverCount;
        private long networkCount;
        private long storageCount;
        private long endpointCount;
        private long cloudCount;
        private long databaseCount;
        private long atRiskCount;
        private long warrantyExpiringSoonCount;
    }
}
