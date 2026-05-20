package com.smartitsm.common.ai;

import com.smartitsm.common.dto.AIInsightResult;
import com.smartitsm.common.dto.AIScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Client for SmartITSM - handles intelligent ticket routing,
 * priority prediction, asset health analysis, and workflow optimization.
 */
@Slf4j
@Component
public class AIClient {

    private final WebClient webClient;

    @Value("${spring.ai.openai.api-key:EMPTY}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    public AIClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Score ticket priority using AI analysis.
     */
    public AIScoreResult scoreTicket(TicketScoringContext context) {
        log.info("Scoring ticket: {} with AI", context.getTitle());

        String prompt = buildTicketScoringPrompt(context);
        String reasoning = callAI(prompt);

        Double score = extractScoreFromReasoning(reasoning);
        String confidence = determineConfidence(score);

        return AIScoreResult.builder()
                .score(score)
                .reasoning(reasoning)
                .confidence(confidence)
                .factors(extractKeyFactors(context))
                .recommendedAction(determineRecommendedAction(score, context))
                .priorityScore(score / 10.0)
                .build();
    }

    /**
     * Classify ticket category using AI.
     */
    public String classifyTicket(TicketClassificationContext context) {
        log.info("Classifying ticket: {}", context.getTitle());

        String prompt = buildTicketClassificationPrompt(context);
        String response = callAI(prompt);

        return extractClassification(response);
    }

    /**
     * Predict ticket resolution time using AI.
     */
    public Integer predictResolutionTime(TicketPredictionContext context) {
        log.info("Predicting resolution time for ticket: {}", context.getTitle());

        String prompt = buildResolutionTimePrompt(context);
        String response = callAI(prompt);

        return extractResolutionTime(response);
    }

    /**
     * Suggest best assignment using AI.
     */
    public AssignmentSuggestion suggestAssignment(TicketAssignmentContext context) {
        log.info("Suggesting assignment for ticket: {}", context.getTitle());

        String prompt = buildAssignmentPrompt(context);
        String response = callAI(prompt);

        return parseAssignmentSuggestion(response, context);
    }

    /**
     * Analyze asset health using AI.
     */
    public AssetHealthAnalysis analyzeAssetHealth(AssetHealthContext context) {
        log.info("Analyzing asset health for: {}", context.getAssetName());

        String prompt = buildAssetHealthPrompt(context);
        String response = callAI(prompt);

        return parseAssetHealthAnalysis(response, context);
    }

    /**
     * Optimize workflow using AI.
     */
    public AIInsightResult optimizeWorkflow(WorkflowOptimizationContext context) {
        log.info("Optimizing workflow: {}", context.getWorkflowName());

        String prompt = buildWorkflowOptimizationPrompt(context);
        String response = callAI(prompt);

        return parseWorkflowInsight(response, context);
    }

    /**
     * Detect anomalies in system metrics.
     */
    public AIInsightResult detectAnomaly(AnomalyDetectionContext context) {
        log.info("Detecting anomalies in metric: {}", context.getMetricName());

        String prompt = buildAnomalyDetectionPrompt(context);
        String response = callAI(prompt);

        return parseAnomalyInsight(response, context);
    }

    // ==================== AI Prompt Builders ====================

    private String buildTicketScoringPrompt(TicketScoringContext context) {
        return String.format("""
            Analyze the following IT ticket and provide a priority score from 0-100 with detailed reasoning.

            Ticket Information:
            - Title: %s
            - Description: %s
            - Category: %s
            - Urgency: %s
            - Impact: %s
            - Requester: %s (priority level: %s)
            - Assignment Group: %s

            Technical Details:
            - Affected Users: %d
            - Business Value: %s
            - Downtime Impact: %s
            - SLA Tier: %s

            Historical Context:
            - Similar tickets resolved in avg %.1f hours
            - Current backlog in group: %d tickets
            - Escalation rate for category: %.1f%%

            Provide your response in JSON format:
            {
              "score": <0-100>,
              "reasoning": "<detailed explanation>",
              "key_factors": ["factor1", "factor2", "factor3"],
              "recommended_action": "<next best action>"
            }
            """,
            context.getTitle(),
            context.getDescription(),
            context.getCategory(),
            context.getUrgency(),
            context.getImpact(),
            context.getRequesterName(),
            context.getRequesterPriority(),
            context.getAssignmentGroup(),
            context.getAffectedUsers(),
            context.getBusinessValue(),
            context.getDowntimeImpact(),
            context.getSlaTier(),
            context.getAvgResolutionHours(),
            context.getGroupBacklog(),
            context.getEscalationRate()
        );
    }

    private String buildTicketClassificationPrompt(TicketClassificationContext context) {
        return String.format("""
            Classify this IT ticket into one of the following categories:
            - INCIDENT: Technical issues, system failures, errors
            - SERVICE_REQUEST: User requests for information, access, services
            - CHANGE_REQUEST: Requests for modifications to systems or services
            - PROBLEM: Underlying cause analysis for recurring incidents
            - KNOWLEDGE_ARTICLE: Documentation or procedure creation

            Ticket:
            - Title: %s
            - Description: %s
            - Keywords: %s

            Also suggest:
            1. Primary category
            2. Sub-category (if applicable)
            3. Required assignment group
            4. Any missing information

            Return JSON:
            {
              "category": "<category>",
              "subCategory": "<sub-category>",
              "confidence": "<HIGH/MEDIUM/LOW>",
              "reasoning": "<brief explanation>"
            }
            """,
            context.getTitle(),
            context.getDescription(),
            context.getKeywords() != null ? String.join(", ", context.getKeywords()) : "none"
        );
    }

    private String buildResolutionTimePrompt(TicketPredictionContext context) {
        return String.format("""
            Predict the resolution time (in hours) for this IT ticket.

            Ticket:
            - Title: %s
            - Description: %s
            - Category: %s
            - Priority: %s
            - Complexity: %s
            - Assigned to: %s (skill level: %s)

            Context:
            - Similar tickets took: %.1f avg hours
            - Current queue for assignee: %d tickets
            - Urgency: %s

            Return JSON:
            {
              "estimatedHours": <number>,
              "minHours": <number>,
              "maxHours": <number>,
              "reasoning": "<explanation>",
              "confidence": "<HIGH/MEDIUM/LOW>"
            }
            """,
            context.getTitle(),
            context.getDescription(),
            context.getCategory(),
            context.getPriority(),
            context.getComplexity(),
            context.getAssigneeName(),
            context.getAssigneeSkillLevel(),
            context.getAvgResolutionHours(),
            context.getAssigneeQueueSize(),
            context.getUrgency()
        );
    }

    private String buildAssignmentPrompt(TicketAssignmentContext context) {
        return String.format("""
            Suggest the best assignment for this IT ticket.

            Ticket:
            - Title: %s
            - Category: %s
            - Complexity: %s
            - Priority: %d
            - Skills required: %s

            Available Agents:
            %s

            Consider:
            1. Skill match
            2. Current workload
            3. Historical performance on similar tickets
            4. Availability and response time

            Return JSON:
            {
              "recommendedAgent": "<agent name or 'Escalate'>",
              "reasoning": "<why this assignment>",
              "alternativeAgents": ["agent2", "agent3"],
              "estimatedResolution": "<hours>"
            }
            """,
            context.getTitle(),
            context.getCategory(),
            context.getComplexity(),
            context.getPriorityScore(),
            context.getRequiredSkills() != null ? String.join(", ", context.getRequiredSkills()) : "general",
            formatAgentList(context.getAvailableAgents())
        );
    }

    private String buildAssetHealthPrompt(AssetHealthContext context) {
        return String.format("""
            Analyze the health and risk of this IT asset.

            Asset Information:
            - Name: %s
            - Type: %s
            - Manufacturer: %s
            - Model: %s
            - Age: %d years
            - Purchase Date: %s
            - Warranty: %s

            Current Metrics:
            - CPU Usage: %.1f%%
            - Memory Usage: %.1f%%
            - Disk Usage: %.1f%%
            - Network Latency: %.1fms
            - Error Rate: %.2f%%
            - Incident Count (last 30 days): %d
            - Maintenance Compliance: %.1f%%

            Risk Assessment:
            - Failure Probability: %.1f%%
            - Business Impact: %s

            Return JSON:
            {
              "healthScore": <0-100>,
              "riskLevel": "<CRITICAL/HIGH/MEDIUM/LOW>",
              "prediction": "<failure timeline if any>",
              "recommendations": ["action1", "action2"],
              "maintenanceWindow": "<suggested date>",
              "estimatedReplacementCost": <number>
            }
            """,
            context.getAssetName(),
            context.getAssetType(),
            context.getManufacturer(),
            context.getModel(),
            context.getAgeYears(),
            context.getPurchaseDate(),
            context.getWarrantyStatus(),
            context.getCpuUsage(),
            context.getMemoryUsage(),
            context.getDiskUsage(),
            context.getNetworkLatency(),
            context.getErrorRate(),
            context.getIncidentCountLast30Days(),
            context.getMaintenanceCompliance(),
            context.getFailureProbability(),
            context.getBusinessImpact()
        );
    }

    private String buildWorkflowOptimizationPrompt(WorkflowOptimizationContext context) {
        return String.format("""
            Analyze and optimize this ITSM workflow.

            Workflow Information:
            - Name: %s
            - Current Steps: %d
            - Average Completion Time: %.1f hours
            - Current SLA Compliance: %.1f%%
            - Current Cost: $%.2f

            Step Details:
            %s

            Bottlenecks Identified:
            %s

            Provide optimization recommendations considering:
            1. Step elimination or consolidation
            2. Parallel processing opportunities
            3. Automation potential
            4. SLA improvement
            5. Cost reduction

            Return JSON:
            {
              "currentEfficiency": <0-100>,
              "recommendedSteps": ["step1", "step2"],
              "estimatedTimeSavings": "<hours>",
              "estimatedCostSavings": <dollars>,
              "slaImprovement": "<percentage>",
              "automationOpportunities": ["auto1", "auto2"],
              "riskAssessment": "<brief>"
            }
            """,
            context.getWorkflowName(),
            context.getTotalSteps(),
            context.getAvgCompletionHours(),
            context.getSlaComplianceRate(),
            context.getCurrentCost(),
            formatStepDetails(context.getStepDetails()),
            context.getBottlenecks() != null ? String.join("\n", context.getBottlenecks()) : "none identified"
        );
    }

    private String buildAnomalyDetectionPrompt(AnomalyDetectionContext context) {
        return String.format("""
            Detect anomalies in this system metric.

            Metric Information:
            - Name: %s
            - Current Value: %.2f
            - Baseline: %.2f
            - Threshold: %.2f
            - Unit: %s

            Time Series Data (last 7 days):
            %s

            Context:
            - Description: %s
            - Severity if breached: %s

            Analyze for:
            1. Statistical anomalies
            2. Trend changes
            3. Seasonal patterns
            4. Correlation with other events

            Return JSON:
            {
              "isAnomaly": <true/false>,
              "anomalyType": "<SPIKE/DROP/TREND/SEASONAL>",
              "confidence": <0-1>,
              "severity": "<CRITICAL/WARNING/INFO>",
              "description": "<what changed>",
              "potentialCauses": ["cause1", "cause2"],
              "recommendedActions": ["action1", "action2"],
              "predictedDuration": "<if ongoing>"
            }
            """,
            context.getMetricName(),
            context.getCurrentValue(),
            context.getBaseline(),
            context.getThreshold(),
            context.getUnit(),
            formatTimeSeriesData(context.getTimeSeriesData()),
            context.getDescription(),
            context.getSeverityIfBreached()
        );
    }

    // ==================== AI Response Parsing ====================

    private String callAI(String prompt) {
        if ("EMPTY".equals(apiKey)) {
            log.warn("AI API key not configured, returning default response");
            return "{\"score\": 50, \"reasoning\": \"AI analysis unavailable\"}";
        }

        try {
            Map<String, Object> request = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3
            );

            String response = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parseAIResponse(response);
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage());
            return "{\"error\": \"AI analysis unavailable\"}";
        }
    }

    private String parseAIResponse(String response) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response);
            if (json.containsKey("choices")) {
                var choices = json.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0).getJSONObject("message").getString("content");
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
        }
        return response;
    }

    private Double extractScoreFromReasoning(String reasoning) {
        Pattern pattern = Pattern.compile("\"score\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(reasoning);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 50.0;
            }
        }
        return 50.0;
    }

    private String determineConfidence(Double score) {
        if (score >= 80 || score <= 20) return "HIGH";
        if (score >= 40 && score <= 60) return "MEDIUM";
        return "LOW";
    }

    private String[] extractKeyFactors(TicketScoringContext context) {
        List<String> factors = new ArrayList<>();
        factors.add("Urgency: " + context.getUrgency());
        factors.add("Impact: " + context.getImpact());
        factors.add("Affected Users: " + context.getAffectedUsers());
        if (context.getSlaTier() != null) {
            factors.add("SLA: " + context.getSlaTier());
        }
        return factors.toArray(new String[0]);
    }

    private String determineRecommendedAction(Double score, TicketScoringContext context) {
        if (score >= 90) {
            return "IMMEDIATE - Escalate to L2/L3 support and notify management";
        } else if (score >= 70) {
            return "HIGH PRIORITY - Assign to senior agent within 15 minutes";
        } else if (score >= 50) {
            return "MEDIUM PRIORITY - Standard queue processing";
        } else {
            return "LOW PRIORITY - Batch processing acceptable";
        }
    }

    private String extractClassification(String response) {
        Pattern pattern = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "SERVICE_REQUEST";
    }

    private Integer extractResolutionTime(String response) {
        Pattern pattern = Pattern.compile("\"estimatedHours\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 4;
            }
        }
        return 4;
    }

    private String formatAgentList(List<Map<String, Object>> agents) {
        if (agents == null || agents.isEmpty()) {
            return "No agents available";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> agent : agents) {
            sb.append(String.format("- %s: skills=%s, workload=%s, success_rate=%.1f%%\n",
                agent.getOrDefault("name", "Unknown"),
                agent.getOrDefault("skills", "general"),
                agent.getOrDefault("currentWorkload", "unknown"),
                ((Number) agent.getOrDefault("successRate", 0)).doubleValue()));
        }
        return sb.toString();
    }

    private String formatStepDetails(List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty()) {
            return "No step details available";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            sb.append(String.format("Step %d: %s (avg %.1f hours)\n",
                i + 1,
                step.getOrDefault("name", "Unknown"),
                ((Number) step.getOrDefault("avgHours", 0)).doubleValue()));
        }
        return sb.toString();
    }

    private String formatTimeSeriesData(List<Map<String, Object>> timeSeriesData) {
        if (timeSeriesData == null || timeSeriesData.isEmpty()) {
            return "No historical data available";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> point : timeSeriesData) {
            sb.append(String.format("- %s: %.2f\n",
                point.getOrDefault("timestamp", "unknown"),
                ((Number) point.getOrDefault("value", 0)).doubleValue()));
        }
        return sb.toString();
    }

    private AssignmentSuggestion parseAssignmentSuggestion(String response, TicketAssignmentContext context) {
        String agent = "DEFAULT_AGENT";
        Pattern pattern = Pattern.compile("\"recommendedAgent\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            agent = matcher.group(1);
        }
        return AssignmentSuggestion.builder()
                .suggestedAgentId(agent)
                .reasoning(response)
                .confidence("MEDIUM")
                .build();
    }

    private AssetHealthAnalysis parseAssetHealthAnalysis(String response, AssetHealthContext context) {
        int healthScore = 50;
        String riskLevel = "MEDIUM";
        Pattern scorePattern = Pattern.compile("\"healthScore\"\\s*:\\s*(\\d+)");
        Matcher scoreMatcher = scorePattern.matcher(response);
        if (scoreMatcher.find()) {
            healthScore = Integer.parseInt(scoreMatcher.group(1));
        }
        Pattern riskPattern = Pattern.compile("\"riskLevel\"\\s*:\\s*\"([^\"]+)\"");
        Matcher riskMatcher = riskPattern.matcher(response);
        if (riskMatcher.find()) {
            riskLevel = riskMatcher.group(1);
        }
        return AssetHealthAnalysis.builder()
                .healthScore((double) healthScore)
                .riskLevel(riskLevel)
                .prediction("Normal operation")
                .recommendations(List.of("Continue monitoring"))
                .build();
    }

    private AIInsightResult parseWorkflowInsight(String response, WorkflowOptimizationContext context) {
        return AIInsightResult.builder()
                .insight(response)
                .confidence(0.5)
                .category("WORKFLOW_OPTIMIZATION")
                .build();
    }

    private AIInsightResult parseAnomalyInsight(String response, AnomalyDetectionContext context) {
        boolean isAnomaly = false;
        Pattern pattern = Pattern.compile("\"isAnomaly\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            isAnomaly = Boolean.parseBoolean(matcher.group(1));
        }
        return AIInsightResult.builder()
                .insight(response)
                .confidence(isAnomaly ? 0.9 : 0.3)
                .category("ANOMALY_DETECTION")
                .build();
    }

    // ==================== Context Classes ====================

    @lombok.Data
    @lombok.Builder
    public static class TicketScoringContext {
        private String title;
        private String description;
        private String category;
        private String urgency;
        private String impact;
        private String requesterName;
        private String requesterPriority;
        private String assignmentGroup;
        private int affectedUsers;
        private String businessValue;
        private String downtimeImpact;
        private String slaTier;
        private double avgResolutionHours;
        private int groupBacklog;
        private double escalationRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class TicketClassificationContext {
        private String title;
        private String description;
        private List<String> keywords;
    }

    @lombok.Data
    @lombok.Builder
    public static class TicketPredictionContext {
        private String title;
        private String description;
        private String category;
        private String priority;
        private String complexity;
        private String assigneeName;
        private String assigneeSkillLevel;
        private double avgResolutionHours;
        private int assigneeQueueSize;
        private String urgency;
    }

    @lombok.Data
    @lombok.Builder
    public static class TicketAssignmentContext {
        private String title;
        private String category;
        private String complexity;
        private int priorityScore;
        private List<String> requiredSkills;
        private List<Map<String, Object>> availableAgents;
    }

    @lombok.Data
    @lombok.Builder
    public static class AssetHealthContext {
        private String assetName;
        private String assetType;
        private String manufacturer;
        private String model;
        private int ageYears;
        private LocalDate purchaseDate;
        private String warrantyStatus;
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private double networkLatency;
        private double errorRate;
        private int incidentCountLast30Days;
        private double maintenanceCompliance;
        private double failureProbability;
        private String businessImpact;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkflowOptimizationContext {
        private String workflowName;
        private int totalSteps;
        private double avgCompletionHours;
        private double slaComplianceRate;
        private double currentCost;
        private List<Map<String, Object>> stepDetails;
        private List<String> bottlenecks;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnomalyDetectionContext {
        private String metricName;
        private double currentValue;
        private double baseline;
        private double threshold;
        private String unit;
        private List<Map<String, Object>> timeSeriesData;
        private String description;
        private String severityIfBreached;
    }

    @lombok.Data
    @lombok.Builder
    public static class AssignmentSuggestion {
        private String suggestedAgentId;
        private String reasoning;
        private String confidence;
        private String recommendedAgent;
    }

    @lombok.Data
    @lombok.Builder
    public static class AgentInfo {
        private String name;
        private int currentWorkload;
        private double avgResolutionHours;
        private double successRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class AssetHealthAnalysis {
        private double healthScore;
        private String riskLevel;
        private String prediction;
        private List<String> recommendations;
    }
}
