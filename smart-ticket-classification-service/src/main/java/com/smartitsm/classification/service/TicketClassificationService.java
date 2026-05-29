package com.smartitsm.classification.service;

import com.smartitsm.classification.dto.ClassificationRequest;
import com.smartitsm.classification.dto.ClassificationResponse;
import com.smartitsm.classification.dto.ClassificationStats;
import com.smartitsm.classification.entity.TicketClassification;
import com.smartitsm.classification.repository.TicketClassificationRepository;
import com.smartitsm.common.ai.AIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Smart ticket classification service with keyword-based and AI-powered classification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketClassificationService {

    private final TicketClassificationRepository classificationRepository;
    private final AIClient aiClient;

    // Keyword patterns for classification
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
        "INCIDENT", Arrays.asList(
            "down", "error", "fail", "crash", "not working", "broken", "issue",
            "urgent", "emergency", "critical", "outage", "cannot access", "blocked"
        ),
        "SERVICE_REQUEST", Arrays.asList(
            "request", "please", "need", "want", "setup", "create", "add",
            "access", "password", "reset", "enable", "provide", "approve"
        ),
        "CHANGE_REQUEST", Arrays.asList(
            "change", "modify", "update", "upgrade", "replace", "migrate",
            "configure", "adjust", "alter", "revision", "patch"
        ),
        "PROBLEM", Arrays.asList(
            "recurring", "persistent", "repeated", "chronic", "investigation",
            "root cause", "analysis", "pattern", "unstable"
        )
    );

    private static final Map<String, List<String>> PRIORITY_KEYWORDS = Map.of(
        "CRITICAL", Arrays.asList(
            "production down", "全体宕机", "complete outage", "data loss",
            "security breach", "ransomware", "critical business"
        ),
        "HIGH", Arrays.asList(
            "urgent", "important", "high priority", "asap", "immediately",
            "business critical", "many users affected"
        ),
        "MEDIUM", Arrays.asList(
            "normal", "standard", "when possible", "non-urgent"
        ),
        "LOW", Arrays.asList(
            "minor", "cosmetic", "enhancement", "future", "nice to have",
            "low priority", "background"
        )
    );

    private static final Map<String, String> GROUP_ROUTING = Map.of(
        "INCIDENT", "L2_Support",
        "SERVICE_REQUEST", "L1_Service_Desk",
        "CHANGE_REQUEST", "Change_Management_Team",
        "PROBLEM", "Problem_Management_Team"
    );

    /**
     * Classify a ticket using hybrid approach (keyword + AI).
     */
    @Transactional
    public ClassificationResponse classifyTicket(ClassificationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Keyword-based initial classification
            KeywordResult keywordResult = classifyByKeywords(request);

            // Step 2: AI-enhanced classification for better accuracy
            AIResult aiResult = classifyByAI(request, keywordResult);

            // Step 3: Determine final classification
            String finalCategory = determineFinalCategory(keywordResult, aiResult);
            String finalPriority = determineFinalPriority(keywordResult, aiResult);
            String assignedGroup = determineAssignmentGroup(finalCategory);
            String suggestedAssignee = determineSuggestedAssignee(assignedGroup, finalPriority);

            // Step 4: Save classification record
            TicketClassification classification = saveClassification(request, keywordResult, aiResult,
                finalCategory, finalPriority, assignedGroup, suggestedAssignee);

            // Build response
            return ClassificationResponse.builder()
                .ticketId(request.getTicketId())
                .ticketNumber(request.getTicketNumber())
                .category(finalCategory)
                .subCategory(keywordResult.subCategory)
                .categoryConfidence(aiResult.confidence)
                .classificationMethod(aiResult.usedAI ? "HYBRID" : "KEYWORD")
                .priority(finalPriority)
                .priorityScore(calculatePriorityScore(finalPriority))
                .priorityReasoning(aiResult.reasoning)
                .assignedGroup(assignedGroup)
                .suggestedAssignee(suggestedAssignee)
                .routingConfidence(aiResult.confidence)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .status("SUCCESS")
                .message("Classification completed successfully")
                .build();

        } catch (Exception e) {
            log.error("Classification failed for ticket {}: {}", request.getTicketNumber(), e.getMessage());
            return ClassificationResponse.builder()
                .ticketId(request.getTicketId())
                .ticketNumber(request.getTicketNumber())
                .status("FAILED")
                .message("Classification failed: " + e.getMessage())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * Classify ticket based on keywords in title and description.
     */
    private KeywordResult classifyByKeywords(ClassificationRequest request) {
        String text = ((request.getTitle() != null ? request.getTitle() : "") + " " +
                       (request.getDescription() != null ? request.getDescription() : "")).toLowerCase();

        String category = "SERVICE_REQUEST";
        int categoryScore = 0;
        String subCategory = null;
        Set<String> matchedKeywords = new HashSet<>();

        // Check category keywords
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    score++;
                    matchedKeywords.add(keyword);
                }
            }
            if (score > categoryScore) {
                categoryScore = score;
                category = entry.getKey();
            }
        }

        // Check priority keywords
        String priority = "MEDIUM";
        int priorityScore = 0;
        for (Map.Entry<String, List<String>> entry : PRIORITY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    score++;
                    matchedKeywords.add(keyword);
                }
            }
            if (score > priorityScore) {
                priorityScore = score;
                priority = entry.getKey();
            }
        }

        // Adjust for requester priority
        if ("VIP".equals(request.getRequesterPriority())) {
            if ("LOW".equals(priority)) priority = "MEDIUM";
            else if ("MEDIUM".equals(priority)) priority = "HIGH";
        }

        // Adjust for affected users
        if (request.getAffectedUsers() != null && request.getAffectedUsers() > 10) {
            if ("LOW".equals(priority)) priority = "MEDIUM";
            if ("MEDIUM".equals(priority)) priority = "HIGH";
        }

        return new KeywordResult(category, subCategory, priority, matchedKeywords, categoryScore);
    }

    /**
     * Enhance classification using AI.
     */
    private AIResult classifyByAI(ClassificationRequest request, KeywordResult keywordResult) {
        try {
            AIClient.TicketClassificationContext context = AIClient.TicketClassificationContext.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .keywords(new ArrayList<>(keywordResult.matchedKeywords))
                .build();

            String aiCategory = aiClient.classifyTicket(context);

            // Determine confidence based on AI response
            double confidence = 0.75;
            String reasoning = "AI-enhanced classification";

            if (aiCategory != null && !aiCategory.isEmpty()) {
                return new AIResult(aiCategory, confidence, reasoning, true);
            }
        } catch (Exception e) {
            log.warn("AI classification failed, using keyword result: {}", e.getMessage());
        }

        return new AIResult(keywordResult.category, 0.6, "Keyword-based classification", false);
    }

    /**
     * Determine final category by combining keyword and AI results.
     */
    private String determineFinalCategory(KeywordResult keywordResult, AIResult aiResult) {
        if (aiResult.usedAI && aiResult.confidence > 0.8) {
            return aiResult.category;
        }
        return keywordResult.category;
    }

    /**
     * Determine final priority.
     */
    private String determineFinalPriority(KeywordResult keywordResult, AIResult aiResult) {
        // Use the higher priority between keyword and AI results
        int keywordPriority = getPriorityRank(keywordResult.priority);
        // AI result doesn't include priority, so use keyword result
        return keywordResult.priority;
    }

    private int getPriorityRank(String priority) {
        return switch (priority) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 2;
        };
    }

    private String determineAssignmentGroup(String category) {
        return GROUP_ROUTING.getOrDefault(category, "L1_Service_Desk");
    }

    private String determineSuggestedAssignee(String group, String priority) {
        // Simple load balancing simulation
        String[] agents = {"agent1", "agent2", "agent3"};
        int index = (int) (System.currentTimeMillis() % agents.length);
        return agents[index];
    }

    private double calculatePriorityScore(String priority) {
        return switch (priority) {
            case "CRITICAL" -> 90.0;
            case "HIGH" -> 70.0;
            case "MEDIUM" -> 50.0;
            case "LOW" -> 30.0;
            default -> 50.0;
        };
    }

    private TicketClassification saveClassification(ClassificationRequest request,
            KeywordResult keywordResult, AIResult aiResult, String category, String priority,
            String assignedGroup, String suggestedAssignee) {

        TicketClassification classification = TicketClassification.builder()
            .ticketId(request.getTicketId())
            .ticketNumber(request.getTicketNumber())
            .category(category)
            .subCategory(keywordResult.subCategory)
            .categoryConfidence(aiResult.confidence)
            .classificationMethod(aiResult.usedAI ? "HYBRID" : "KEYWORD")
            .priority(priority)
            .priorityScore(calculatePriorityScore(priority))
            .priorityReasoning(aiResult.reasoning)
            .assignedGroup(assignedGroup)
            .suggestedAssignee(suggestedAssignee)
            .routingConfidence(aiResult.confidence)
            .routingReasoning(aiResult.reasoning)
            .aiModel(aiResult.usedAI ? "deepseek" : null)
            .matchedKeywords(String.join(",", keywordResult.matchedKeywords))
            .aiProcessedAt(LocalDateTime.now())
            .build();

        classificationRepository.save(classification);
        return classification;
    }

    /**
     * Get classification statistics.
     */
    public ClassificationStats getClassificationStats() {
        // This would typically query the database for statistics
        // For now, return a placeholder
        return ClassificationStats.builder()
            .totalClassified(0L)
            .autoClassified(0L)
            .aiClassified(0L)
            .keywordClassified(0L)
            .avgConfidence(0.0)
            .autoClassificationRate(0.0)
            .build();
    }

    /**
     * Get classification by ticket ID.
     */
    public TicketClassification getClassificationByTicketId(Long ticketId) {
        return classificationRepository.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TicketClassification>()
                .eq("ticket_id", ticketId)
                .orderByDesc("created_at")
                .last("LIMIT 1")
        );
    }

    // Inner classes for intermediate results
    private record KeywordResult(String category, String subCategory, String priority,
                                   Set<String> matchedKeywords, int score) {}

    private record AIResult(String category, double confidence, String reasoning, boolean usedAI) {}
}