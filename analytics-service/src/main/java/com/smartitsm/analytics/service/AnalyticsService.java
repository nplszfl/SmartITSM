package com.smartitsm.analytics.service;

import com.smartitsm.analytics.dto.*;
import com.smartitsm.common.ai.AIClient;
import com.smartitsm.common.dto.AIInsightResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Analytics Service - business logic for ITSM metrics and reporting.
 * Provides aggregated analytics across tickets, assets, and workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AIClient aiClient;
    private final WebClient.Builder webClientBuilder;

    // Base URLs for inter-service communication
    private static final String TICKET_SERVICE_URL = "http://ticket-service:8081";
    private static final String ASSET_SERVICE_URL = "http://asset-service:8082";
    private static final String WORKFLOW_SERVICE_URL = "http://workflow-service:8083";

    // ==================== Dashboard Summary ====================

    /**
     * Get full analytics summary for dashboard by aggregating data from all services.
     */
    @SuppressWarnings("unchecked")
    public AnalyticsSummary getSummary() {
        log.info("Generating analytics summary");
        AnalyticsSummary.AnalyticsSummaryBuilder builder = AnalyticsSummary.builder()
                .generatedAt(LocalDateTime.now());

        try {
            // Ticket metrics from ticket service
            Map<String, Object> ticketStats = callService(TICKET_SERVICE_URL, "/api/v1/tickets/stats", Map.class);
            if (ticketStats != null) {
                long totalOpen = ((Number) ticketStats.getOrDefault("totalOpen", 0)).longValue();
                builder.openTickets(totalOpen)
                       .breachedSLAs(((Number) ticketStats.getOrDefault("breachedSLA", 0)).longValue());

                // Count total and resolved from ticket list
                Map<String, Object> allTickets = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1", Map.class);
                if (allTickets != null && allTickets.get("total") != null) {
                    long total = ((Number) allTickets.get("total")).longValue();
                    builder.totalTickets(total);
                    builder.resolvedTickets(total - ((Number) ticketStats.getOrDefault("totalOpen", 0)).longValue());
                }
            }

            // Asset stats from asset service
            Map<String, Object> assetStats = callService(ASSET_SERVICE_URL, "/api/v1/assets/stats", Map.class);
            if (assetStats != null) {
                builder.totalAssets(((Number) assetStats.getOrDefault("total", 0)).longValue())
                       .healthyAssets(((Number) assetStats.getOrDefault("healthy", 0)).longValue())
                       .atRiskAssets(((Number) assetStats.getOrDefault("atRisk", 0)).longValue());
            }

            // Workflow stats from workflow service
            Map<String, Object> workflowStats = callService(WORKFLOW_SERVICE_URL, "/api/v1/workflows/stats", Map.class);
            if (workflowStats != null) {
                builder.activeWorkflows(((Number) workflowStats.getOrDefault("running", 0)).longValue())
                       .completedWorkflows(((Number) workflowStats.getOrDefault("completed", 0)).longValue());
            }

            // Calculate average resolution time from resolved tickets
            builder.avgResolutionHours(calculateAvgResolutionHours());

            // Default CSAT if not available
            builder.avgCSAT(0.0);

        } catch (Exception e) {
            log.warn("Failed to fetch analytics summary data, using partial results: {}", e.getMessage());
            // Return what we can with defaults for missing data
            builder.totalTickets(0L).openTickets(0L).resolvedTickets(0L).breachedSLAs(0L)
                   .totalAssets(0L).healthyAssets(0L).atRiskAssets(0L)
                   .activeWorkflows(0L).completedWorkflows(0L)
                   .avgResolutionHours(0.0).avgCSAT(0.0);
        }

        return builder.build();
    }

    // ==================== Ticket Analytics ====================

    /**
     * Get ticket creation/resolution trends over time by querying ticket history.
     */
    @SuppressWarnings("unchecked")
    public List<TicketTrend> getTicketTrends(int days) {
        List<TicketTrend> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try {
            // Get all tickets to analyze trends
            Map<String, Object> response = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (response != null && response.get("records") != null) {
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) response.get("records");

                for (int i = days - 1; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    final LocalDate targetDate = date;

                    long created = tickets.stream()
                            .filter(t -> isOnDate(parseDate(t.get("createdAt")), targetDate))
                            .count();

                    long resolved = tickets.stream()
                            .filter(t -> isOnDate(parseDate(t.get("resolvedAt")), targetDate))
                            .count();

                    long breached = tickets.stream()
                            .filter(t -> isOnDate(parseDate(t.get("createdAt")), targetDate))
                            .filter(t -> {
                                Object sla = t.get("slaBreached");
                                return sla != null && Boolean.TRUE.equals(sla);
                            })
                            .count();

                    trends.add(TicketTrend.builder()
                            .date(date.format(DateTimeFormatter.ISO_DATE))
                            .created((int) created)
                            .resolved((int) resolved)
                            .breached((int) breached)
                            .build());
                }
            } else {
                // Fallback: return empty trends
                for (int i = days - 1; i >= 0; i--) {
                    trends.add(TicketTrend.builder()
                            .date(today.minusDays(i).format(DateTimeFormatter.ISO_DATE))
                            .created(0).resolved(0).breached(0)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch ticket trends: {}", e.getMessage());
            for (int i = days - 1; i >= 0; i--) {
                trends.add(TicketTrend.builder()
                        .date(today.minusDays(i).format(DateTimeFormatter.ISO_DATE))
                        .created(0).resolved(0).breached(0)
                        .build());
            }
        }

        return trends;
    }

    /**
     * Get ticket breakdown by category (INCIDENT, SERVICE_REQUEST, CHANGE_REQUEST, PROBLEM).
     */
    @SuppressWarnings("unchecked")
    public List<CategoryBreakdown> getCategoryBreakdown() {
        List<CategoryBreakdown> breakdown = new ArrayList<>();
        String[] categories = {"INCIDENT", "SERVICE_REQUEST", "CHANGE_REQUEST", "PROBLEM"};

        try {
            Map<String, Object> allTickets = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (allTickets != null && allTickets.get("records") != null) {
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) allTickets.get("records");

                for (String category : categories) {
                    List<Map<String, Object>> categoryTickets = tickets.stream()
                            .filter(t -> category.equals(t.get("category")))
                            .toList();

                    long count = categoryTickets.size();
                    long resolved = categoryTickets.stream()
                            .filter(t -> "RESOLVED".equals(t.get("status")) || "CLOSED".equals(t.get("status")))
                            .count();

                    double avgHours = calculateAvgResolutionHoursForTickets(categoryTickets);

                    long breached = categoryTickets.stream()
                            .filter(t -> {
                                Object sla = t.get("slaBreached");
                                return sla != null && Boolean.TRUE.equals(sla);
                            })
                            .count();

                    breakdown.add(CategoryBreakdown.builder()
                            .category(category)
                            .count(count)
                            .resolved(resolved)
                            .avgResolutionHours(avgHours)
                            .breachedCount(breached)
                            .build());
                }
            } else {
                for (String category : categories) {
                    breakdown.add(CategoryBreakdown.builder()
                            .category(category).count(0).resolved(0)
                            .avgResolutionHours(0).breachedCount(0)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch category breakdown: {}", e.getMessage());
            for (String category : categories) {
                breakdown.add(CategoryBreakdown.builder()
                        .category(category).count(0).resolved(0)
                        .avgResolutionHours(0).breachedCount(0)
                        .build());
            }
        }

        return breakdown;
    }

    /**
     * Get agent performance metrics by aggregating ticket assignments and resolutions.
     */
    @SuppressWarnings("unchecked")
    public List<AgentPerformance> getAgentPerformance(int days) {
        List<AgentPerformance> performances = new ArrayList<>();

        try {
            Map<String, Object> allTickets = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (allTickets != null && allTickets.get("records") != null) {
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) allTickets.get("records");
                LocalDate cutoff = LocalDate.now().minusDays(days);

                // Group tickets by assignee
                Map<String, List<Map<String, Object>>> byAgent = new HashMap<>();
                for (Map<String, Object> ticket : tickets) {
                    String assignee = String.valueOf(ticket.getOrDefault("assignedTo", "unassigned"));
                    byAgent.computeIfAbsent(assignee, k -> new ArrayList<>()).add(ticket);
                }

                for (Map.Entry<String, List<Map<String, Object>>> entry : byAgent.entrySet()) {
                    String agentName = entry.getKey();
                    List<Map<String, Object>> agentTickets = entry.getValue();

                    long assigned = agentTickets.stream()
                            .filter(t -> isAfter(parseDate(t.get("createdAt")), cutoff))
                            .count();

                    long resolved = agentTickets.stream()
                            .filter(t -> isAfter(parseDate(t.get("resolvedAt")), cutoff))
                            .filter(t -> "RESOLVED".equals(t.get("status")) || "CLOSED".equals(t.get("status")))
                            .count();

                    long breached = agentTickets.stream()
                            .filter(t -> {
                                Object sla = t.get("slaBreached");
                                return sla != null && Boolean.TRUE.equals(sla);
                            })
                            .count();

                    double avgResolution = calculateAvgResolutionHoursForTickets(
                            agentTickets.stream()
                                    .filter(t -> "RESOLVED".equals(t.get("status")) || "CLOSED".equals(t.get("status")))
                                    .toList());

                    performances.add(AgentPerformance.builder()
                            .agentName(agentName)
                            .ticketsAssigned(assigned)
                            .ticketsResolved(resolved)
                            .resolutionRate(resolved > 0 ? Math.round((double) resolved / assigned * 100) : 0.0)
                            .avgResolutionHours(avgResolution)
                            .avgCSAT(0.0)
                            .breachedSLAs(breached)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch agent performance: {}", e.getMessage());
        }

        // Sort by tickets resolved descending
        performances.sort((a, b) -> Long.compare(b.getTicketsResolved(), a.getTicketsResolved()));
        return performances;
    }

    // ==================== Asset Analytics ====================

    /**
     * Get asset health summary from asset service.
     */
    @SuppressWarnings("unchecked")
    public AssetHealthSummary getAssetHealthSummary() {
        AssetHealthSummary.AssetHealthSummaryBuilder builder = AssetHealthSummary.builder();

        try {
            Map<String, Object> assetStats = callService(ASSET_SERVICE_URL, "/api/v1/assets/stats", Map.class);
            if (assetStats != null) {
                builder.total(((Number) assetStats.getOrDefault("total", 0)).longValue())
                       .healthy(((Number) assetStats.getOrDefault("healthy", 0)).longValue())
                       .warning(((Number) assetStats.getOrDefault("warning", 0)).longValue())
                       .critical(((Number) assetStats.getOrDefault("critical", 0)).longValue())
                       .atRisk(((Number) assetStats.getOrDefault("atRisk", 0)).longValue())
                       .warrantyExpiring(((Number) assetStats.getOrDefault("warrantyExpiring", 0)).longValue());
            }

            // Get breakdown by type
            Map<String, Object> typeResponse = callService(ASSET_SERVICE_URL, "/api/v1/assets?size=1000", Map.class);
            if (typeResponse != null && typeResponse.get("records") != null) {
                List<Map<String, Object>> assets = (List<Map<String, Object>>) typeResponse.get("records");

                builder.byTypeSERVER(countByType(assets, "SERVER"))
                       .byTypeNETWORK(countByType(assets, "NETWORK"))
                       .byTypeSTORAGE(countByType(assets, "STORAGE"))
                       .byTypeENDPOINT(countByType(assets, "ENDPOINT"))
                       .byTypeCLOUD(countByType(assets, "CLOUD"))
                       .byTypeDATABASE(countByType(assets, "DATABASE"));
            }

        } catch (Exception e) {
            log.warn("Failed to fetch asset health summary: {}", e.getMessage());
            builder.total(0).healthy(0).warning(0).critical(0)
                   .byTypeSERVER(0).byTypeNETWORK(0).byTypeSTORAGE(0)
                   .byTypeENDPOINT(0).byTypeCLOUD(0).byTypeDATABASE(0)
                   .warrantyExpiring(0).atRisk(0);
        }

        return builder.build();
    }

    // ==================== SLA Analytics ====================

    /**
     * Get SLA compliance metrics based on actual ticket data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSLACompliance() {
        Map<String, Object> sla = new LinkedHashMap<>();

        try {
            Map<String, Object> allTickets = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (allTickets != null && allTickets.get("records") != null) {
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) allTickets.get("records");

                // Calculate compliance for each priority level
                sla.put("p1Compliance", calculateSLACompliance(tickets, "CRITICAL"));
                sla.put("p2Compliance", calculateSLACompliance(tickets, "HIGH"));
                sla.put("p3Compliance", calculateSLACompliance(tickets, "MEDIUM"));
                sla.put("p4Compliance", calculateSLACompliance(tickets, "LOW"));

                // Overall compliance
                long totalBreached = tickets.stream()
                        .filter(t -> {
                            Object breached = t.get("slaBreached");
                            return breached != null && Boolean.TRUE.equals(breached);
                        })
                        .count();
                double overall = tickets.isEmpty() ? 100.0 : Math.round((1.0 - (double) totalBreached / tickets.size()) * 100);
                sla.put("firstResponseCompliance", overall);
                sla.put("resolutionCompliance", overall);

            } else {
                setDefaultSLA(sla);
            }
        } catch (Exception e) {
            log.warn("Failed to calculate SLA compliance: {}", e.getMessage());
            setDefaultSLA(sla);
        }

        return sla;
    }

    // ==================== AI Insights ====================

    /**
     * Get AI-generated insights for ITSM operations using DeepSeek.
     */
    public List<String> getAIInsights() {
        List<String> insights = new ArrayList<>();

        try {
            // Collect current metrics for AI analysis
            AnalyticsSummary summary = getSummary();
            Map<String, Object> slaCompliance = getSLACompliance();
            List<CategoryBreakdown> categories = getCategoryBreakdown();

            // Build context for AI analysis
            String mostCommonCategory = categories.stream()
                    .max(Comparator.comparingLong(CategoryBreakdown::getCount))
                    .map(CategoryBreakdown::getCategory)
                    .orElse("INCIDENT");

            long totalTickets = summary.getTotalTickets();
            long openTickets = summary.getOpenTickets();
            long atRiskAssets = summary.getAtRiskAssets();
            double p1Compliance = ((Number) slaCompliance.getOrDefault("p1Compliance", 0)).doubleValue();

            // Use anomaly detection to generate insight about ticket volume
            AIClient.AnomalyDetectionContext context = AIClient.AnomalyDetectionContext.builder()
                    .metricName("Daily Ticket Volume")
                    .currentValue(openTickets)
                    .baseline(totalTickets > 0 ? totalTickets / 30.0 : 20.0)
                    .threshold(totalTickets > 0 ? totalTickets / 10.0 : 50.0)
                    .unit("tickets")
                    .description("ITSM ticket volume analysis")
                    .severityIfBreached("MEDIUM")
                    .build();

            AIInsightResult insight = aiClient.detectAnomaly(context);

            if (insight != null && insight.getDescription() != null) {
                insights.add(insight.getDescription());
            }

            // Add operational insights based on actual data
            if (atRiskAssets > 0) {
                insights.add(String.format("%d assets are at risk and require attention.", atRiskAssets));
            }

            if (p1Compliance < 95.0) {
                insights.add(String.format("P1 SLA compliance at %.1f%% - below target of 95%%.", p1Compliance));
            } else {
                insights.add("P1 SLA compliance is within target range.");
            }

            if (insights.isEmpty()) {
                insights.add("All ITSM metrics are within normal parameters.");
            }

        } catch (Exception e) {
            log.warn("Failed to generate AI insights: {}", e.getMessage());
            insights.add("AI insights temporarily unavailable. Please check service connectivity.");
        }

        // Limit to 3 insights
        return insights.size() > 3 ? insights.subList(0, 3) : insights;
    }

    /**
     * Predict ticket volume for upcoming period based on historical trends.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> predictTicketVolume(int days) {
        Map<String, Integer> prediction = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        try {
            // Get historical ticket data
            Map<String, Object> response = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (response != null && response.get("records") != null) {
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) response.get("records");

                // Calculate daily average from last 30 days
                LocalDate thirtyDaysAgo = today.minusDays(30);
                long recentTickets = tickets.stream()
                        .filter(t -> isAfter(parseDate(t.get("createdAt")), thirtyDaysAgo))
                        .count();
                double dailyAvg = recentTickets / 30.0;

                // Simple moving average prediction
                for (int i = 1; i <= days; i++) {
                    LocalDate date = today.plusDays(i);
                    // Add some variance (±20%) based on day of week
                    double variance = date.getDayOfWeek().getValue() <= 5 ? 1.0 : 0.7;
                    int predicted = (int) Math.round(dailyAvg * variance);
                    prediction.put(date.format(DateTimeFormatter.ISO_DATE), Math.max(0, predicted));
                }
            } else {
                setDefaultPrediction(prediction, today, days);
            }
        } catch (Exception e) {
            log.warn("Failed to predict ticket volume: {}", e.getMessage());
            setDefaultPrediction(prediction, today, days);
        }

        return prediction;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Make a REST call to another microservice.
     */
    private <T> T callService(String baseUrl, String path, Class<T> responseType) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(baseUrl + path)
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.debug("Service call to {} failed: {}", baseUrl + path, e.getMessage());
            return null;
        }
    }

    /**
     * Parse date from various possible formats.
     */
    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) return null;
        try {
            if (dateObj instanceof String s) {
                return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_DATE);
            } else if (dateObj instanceof LocalDate ld) {
                return ld;
            }
        } catch (Exception e) {
            log.trace("Failed to parse date: {}", dateObj);
        }
        return null;
    }

    private boolean isOnDate(LocalDate date, LocalDate target) {
        return date != null && date.equals(target);
    }

    private boolean isAfter(LocalDate date, LocalDate cutoff) {
        return date != null && !date.isBefore(cutoff);
    }

    /**
     * Calculate average resolution hours from resolved tickets.
     */
    private double calculateAvgResolutionHours() {
        try {
            Map<String, Object> response = callService(TICKET_SERVICE_URL, "/api/v1/tickets?size=1000", Map.class);
            if (response != null && response.get("records") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tickets = (List<Map<String, Object>>) response.get("records");
                return calculateAvgResolutionHoursForTickets(
                        tickets.stream()
                                .filter(t -> "RESOLVED".equals(t.get("status")) || "CLOSED".equals(t.get("status")))
                                .toList());
            }
        } catch (Exception e) {
            log.debug("Failed to calculate avg resolution hours: {}", e.getMessage());
        }
        return 0.0;
    }

    private double calculateAvgResolutionHoursForTickets(List<Map<String, Object>> tickets) {
        if (tickets.isEmpty()) return 0.0;

        double totalHours = 0.0;
        int resolvedCount = 0;

        for (Map<String, Object> ticket : tickets) {
            LocalDate created = parseDate(ticket.get("createdAt"));
            LocalDate resolved = parseDate(ticket.get("resolvedAt"));

            if (created != null && resolved != null) {
                long hours = ChronoUnit.HOURS.between(created.atStartOfDay(), resolved.atStartOfDay());
                totalHours += hours;
                resolvedCount++;
            }
        }

        return resolvedCount > 0 ? Math.round(totalHours / resolvedCount * 10) / 10.0 : 0.0;
    }

    private long countByType(List<Map<String, Object>> assets, String type) {
        return assets.stream()
                .filter(a -> type.equals(a.get("assetType")))
                .count();
    }

    private double calculateSLACompliance(List<Map<String, Object>> tickets, String priority) {
        List<Map<String, Object>> priorityTickets = tickets.stream()
                .filter(t -> priority.equals(t.get("priority")))
                .toList();

        if (priorityTickets.isEmpty()) return 100.0;

        long breached = priorityTickets.stream()
                .filter(t -> {
                    Object b = t.get("slaBreached");
                    return b != null && Boolean.TRUE.equals(b);
                })
                .count();

        return Math.round((1.0 - (double) breached / priorityTickets.size()) * 100 * 10) / 10.0;
    }

    private void setDefaultSLA(Map<String, Object> sla) {
        sla.put("firstResponseCompliance", 0.0);
        sla.put("resolutionCompliance", 0.0);
        sla.put("p1Compliance", 0.0);
        sla.put("p2Compliance", 0.0);
        sla.put("p3Compliance", 0.0);
        sla.put("p4Compliance", 0.0);
    }

    private void setDefaultPrediction(Map<String, Integer> prediction, LocalDate today, int days) {
        for (int i = 1; i <= days; i++) {
            prediction.put(today.plusDays(i).format(DateTimeFormatter.ISO_DATE), 0);
        }
    }
}
