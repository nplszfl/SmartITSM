package com.smartitsm.analytics.controller;

import com.smartitsm.analytics.dto.*;
import com.smartitsm.analytics.service.AnalyticsService;
import com.smartitsm.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Analytics REST API Controller.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummary> getSummary() {
        return ApiResponse.ok(analyticsService.getSummary());
    }

    @GetMapping("/tickets/trends")
    public ApiResponse<List<TicketTrend>> getTicketTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.getTicketTrends(days));
    }

    @GetMapping("/tickets/categories")
    public ApiResponse<List<CategoryBreakdown>> getCategoryBreakdown() {
        return ApiResponse.ok(analyticsService.getCategoryBreakdown());
    }

    @GetMapping("/agents/performance")
    public ApiResponse<List<AgentPerformance>> getAgentPerformance(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.getAgentPerformance(days));
    }

    @GetMapping("/assets/health")
    public ApiResponse<AssetHealthSummary> getAssetHealth() {
        return ApiResponse.ok(analyticsService.getAssetHealthSummary());
    }

    @GetMapping("/sla/compliance")
    public ApiResponse<Map<String, Object>> getSLACompliance() {
        return ApiResponse.ok(analyticsService.getSLACompliance());
    }

    @GetMapping("/insights")
    public ApiResponse<List<String>> getInsights() {
        return ApiResponse.ok(analyticsService.getAIInsights());
    }

    @GetMapping("/tickets/prediction")
    public ApiResponse<Map<String, Integer>> predictTicketVolume(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(analyticsService.predictTicketVolume(days));
    }
}
