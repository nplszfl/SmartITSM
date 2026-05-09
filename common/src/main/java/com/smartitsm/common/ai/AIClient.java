package com.smartitsm.common.ai;

import com.smartitsm.common.dto.AIInsightResult;
import com.smartitsm.common.dto.AIScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * AI Client for SmartITSM - handles intelligent ticket routing,
 * priority prediction, asset health analysis, and workflow optimization.
 */
@Slf4j
@Component
public class AIClient {

    private final ChatClient chatClient;

    public AIClient(ChatClient chatClient) {
        this.chatClient = chatClient;
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
                .priorityScore(score / 10.0) // Convert to 0-10 scale
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
        try {
            Prompt aiPrompt = new Prompt(
                new UserMessage(prompt),
                OpenAiChatOptions.builder()
                    .withModel("gpt-4-turbo")
                    .withTemperature(0.3) // Lower temp for more consistent results
                    .build()
            );

            ChatResponse response = chatClient.call(aiPrompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage());
            return "{\"error\": \"AI analysis unavailable\"}";
        }
    }

    private Double extractScoreFromReasoning(String reasoning) {
        if (reasoning.contains("\"score\":")) {
            String[] parts = reasoning.split("\"score\":");
            if (parts.length > 1) {
                String numStr = parts[1].trim().split("[,\\}]")[0];
                try {
                    return Double.parseDouble(numStr);
                } catch (NumberFormatException e) {
                    return 50.0;
                }
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
        if (response.contains("\"category\":")) {
            String[] parts = response.split("\"category\":");
            if (parts.length > 1) {
                String category = parts[1].trim().split("[,\\}]")[0].replace("\"", "").trim();
                return category;
            }
        }
        return "SERVICE_REQUEST";
    }

    private Integer extractResolutionTime(String response) {
        if (response.contains("\"estimatedHours\":")) {
            String[] parts = response.split("\"estimatedHours\":");
            if (parts.length > 1) {
                String numStr = parts[1].trim().split("[,\\}]")[0];
                try {
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException e) {
                    return 24; // Default 24 hours
                }
            }
        }
        return 24;
    }

    private AssignmentSuggestion parseAssignmentSuggestion(String response, TicketAssignmentContext context) {
        String agent = "Unassigned";
        List<String> alternatives = new ArrayList<>();

        if (response.contains("\"recommendedAgent\":")) {
            String[] parts = response.split("\"recommendedAgent\":");
            if (parts.length > 1) {
                agent = parts[1].trim().split("[,\\}]")[0].replace("\"", "").trim();
            }
        }

        if (response.contains("\"alternativeAgents\":")) {
            String[] parts = response.split("\"alternativeAgents\":");
            if (parts.length > 1) {
                String altStr = parts[1].split("]")[0].replace("[", "").replace("\"", "");
                for (String alt : altStr.split(",")) {
                    alternatives.add(alt.trim());
                }
            }
        }

        return AssignmentSuggestion.builder()
                .recommendedAgent(agent)
                .reasoning("AI-based assignment optimization")
                .alternativeAgents(alternatives.toArray(new String[0]))
                .estimatedResolutionHours(24)
                .confidence("HIGH")
                .build();
    }

    private AssetHealthAnalysis parseAssetHealthAnalysis(String response, AssetHealthContext context) {
        Double healthScore = 50.0;
        String riskLevel = "MEDIUM";

        if (response.contains("\"healthScore\":")) {
            String[] parts = response.split("\"healthScore\":");
            if (parts.length > 1) {
                String numStr = parts[1].trim().split("[,\\}]")[0];
                try {
                    healthScore = Double.parseDouble(numStr);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (response.contains("\"riskLevel\":")) {
            String[] parts = response.split("\"riskLevel\":");
            if (parts.length > 1) {
                riskLevel = parts[1].trim().split("[,\\}]")[0].replace("\"", "").trim();
            }
        }

        return AssetHealthAnalysis.builder()
                .healthScore(healthScore)
                .riskLevel(riskLevel)
                .prediction("Monitor for 30 days")
                .recommendations(new String[]{"Schedule maintenance", "Monitor metrics"})
                .confidence("HIGH")
                .build();
    }

    private AIInsightResult parseWorkflowInsight(String response, WorkflowOptimizationContext context) {
        return AIInsightResult.builder()
                .insightType("RECOMMENDATION")
                .title("Workflow Optimization for " + context.getWorkflowName())
                .description(response)
                .confidence(0.85)
                .recommendations(new String[]{"Review step sequence", "Implement automation"})
                .severity("INFO")
                .build();
    }

    private AIInsightResult parseAnomalyInsight(String response, AnomalyDetectionContext context) {
        boolean isAnomaly = response.contains("\"isAnomaly\": true");

        return AIInsightResult.builder()
                .insightType(isAnomaly ? "ANOMALY" : "NORMAL")
                .title("Anomaly Detection: " + context.getMetricName())
                .description(response)
                .confidence(0.80)
                .recommendations(new String[]{"Investigate source", "Check related systems"})
                .severity(isAnomaly ? "WARNING" : "INFO")
                .affectedEntities(new ArrayList<>(List.of(context.getMetricName())))
                .build();
    }

    // ==================== Helper Methods ====================

    private String formatAgentList(List<AgentInfo> agents) {
        StringBuilder sb = new StringBuilder();
        for (AgentInfo agent : agents) {
            sb.append(String.format("- %s: skills=%s, workload=%d tickets, avg resolution=%.1fh\n",
                agent.getName(),
                agent.getSkills() != null ? String.join(",", agent.getSkills()) : "general",
                agent.getCurrentWorkload(),
                agent.getAvgResolutionHours()));
        }
        return sb.toString();
    }

    private String formatStepDetails(List<WorkflowStepInfo> steps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStepInfo step = steps.get(i);
            sb.append(String.format("Step %d: %s (%.1f hours, automation=%s)\n",
                i + 1, step.getName(), step.getDurationHours(), step.getAutomated()));
        }
        return sb.toString();
    }

    private String formatTimeSeriesData(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return "No historical data available";
        }
        StringBuilder sb = new StringBuilder();
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < Math.min(data.size(), labels.length); i++) {
            sb.append(String.format("%s: %.2f\n", labels[i], data.get(i)));
        }
        return sb.toString();
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
        private Integer affectedUsers;
        private String businessValue;
        private String downtimeImpact;
        private String slaTier;
        private Double avgResolutionHours;
        private Integer groupBacklog;
        private Double escalationRate;
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
        private Double avgResolutionHours;
        private Integer assigneeQueueSize;
        private String urgency;
    }

    @lombok.Data
    @lombok.Builder
    public static class TicketAssignmentContext {
        private String title;
        private String category;
        private String complexity;
        private Integer priorityScore;
        private List<String> requiredSkills;
        private List<AgentInfo> availableAgents;
    }

    @lombok.Data
    @lombok.Builder
    public static class AssetHealthContext {
        private String assetName;
        private String assetType;
        private String manufacturer;
        private String model;
        private Integer ageYears;
        private String purchaseDate;
        private String warrantyStatus;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
        private Double networkLatency;
        private Double errorRate;
        private Integer incidentCountLast30Days;
        private Double maintenanceCompliance;
        private Double failureProbability;
        private String businessImpact;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkflowOptimizationContext {
        private String workflowName;
        private Integer totalSteps;
        private Double avgCompletionHours;
        private Double slaComplianceRate;
        private Double currentCost;
        private List<WorkflowStepInfo> stepDetails;
        private List<String> bottlenecks;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnomalyDetectionContext {
        private String metricName;
        private Double currentValue;
        private Double baseline;
        private Double threshold;
        private String unit;
        private List<Double> timeSeriesData;
        private String description;
        private String severityIfBreached;
    }

    @lombok.Data
    @lombok.Builder
    public static class AgentInfo {
        private String name;
        private List<String> skills;
        private Integer currentWorkload;
        private Double avgResolutionHours;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkflowStepInfo {
        private String name;
        private Double durationHours;
        private String automated;
    }

    // ==================== Result Classes ====================

    @lombok.Data
    @lombok.Builder
    public static class AssignmentSuggestion {
        private String recommendedAgent;
        private String reasoning;
        private String[] alternativeAgents;
        private Integer estimatedResolutionHours;
        private String confidence;
    }

    @lombok.Data
    @lombok.Builder
    public static class AssetHealthAnalysis {
        private Double healthScore;
        private String riskLevel;
        private String prediction;
        private String[] recommendations;
        private String maintenanceWindow;
        private Double estimatedReplacementCost;
        private String confidence;
    }
}