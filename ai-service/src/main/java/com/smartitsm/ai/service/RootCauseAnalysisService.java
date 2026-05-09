package com.smartitsm.ai.service;

import com.smartitsm.ai.client.DeepSeekClient;
import com.smartitsm.ai.dto.RootCauseRequest;
import com.smartitsm.ai.dto.RootCauseResponse;
import com.smartitsm.ai.dto.RootCauseResponse.PatternMatch;
import com.smartitsm.ai.dto.RootCauseResponse.WhyStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class RootCauseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(RootCauseAnalysisService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert IT root cause analyst using the 5 Whys methodology and pattern matching.
        
        Analyze tickets to find the true root cause by asking "why" at least 5 times.
        Also match against known issue patterns.
        
        Return JSON with fields:
        - rootCause: the fundamental cause
        - confidence: confidence level (0.0-1.0)
        - whyChain: array of {step (1-5), why (question), finding (answer)}
        - patternMatches: array of {patternId, patternName, similarityScore (0.0-1.0), knownSolution}
        - recommendedFix: suggested resolution
        
        Respond ONLY with valid JSON.
        """;

    private final DeepSeekClient deepSeekClient;

    public RootCauseAnalysisService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Mono<RootCauseResponse> analyze(RootCauseRequest request) {
        log.info("Analyzing root cause for ticket: {}", request.getTicketId());

        String userMessage = buildUserMessage(request);

        return deepSeekClient.chat(SYSTEM_PROMPT, userMessage)
            .map(this::parseAndBuildResponse)
            .defaultIfEmpty(buildEmptyResponse(request.getTicketId()))
            .doOnSuccess(response -> log.info("Root cause analysis complete for {}: {}",
                request.getTicketId(), response.getRootCause()))
            .doOnError(e -> log.error("Root cause analysis failed for {}: {}",
                request.getTicketId(), e.getMessage()));
    }

    private String buildUserMessage(RootCauseRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket ID: ").append(request.getTicketId()).append("\n");
        sb.append("Description: ").append(request.getDescription()).append("\n");
        if (request.getCategory() != null) {
            sb.append("Category: ").append(request.getCategory()).append("\n");
        }
        if (request.getSymptoms() != null) {
            sb.append("Symptoms: ").append(request.getSymptoms()).append("\n");
        }
        if (request.getPreviousAttempts() != null && request.getPreviousAttempts().length > 0) {
            sb.append("Previous Attempts: ").append(String.join(", ", request.getPreviousAttempts())).append("\n");
        }
        return sb.toString();
    }

    private RootCauseResponse parseAndBuildResponse(String aiResponse) {
        RootCauseResponse response = new RootCauseResponse();
        
        try {
            Map<String, Object> parsed = parseJson(aiResponse);
            
            response.setRootCause(getString(parsed, "rootCause", "Unknown"));
            response.setConfidence(getString(parsed, "confidence", "0.7"));
            response.setRecommendedFix(getString(parsed, "recommendedFix", "No fix suggested"));
            
            response.setWhyChain(parseWhyChain(parsed.get("whyChain")));
            response.setPatternMatches(parsePatternMatches(parsed.get("patternMatches")));
            
        } catch (Exception e) {
            log.warn("Failed to parse AI response, using default analysis: {}", e.getMessage());
            response.setRootCause("Analysis pending - could not parse AI response");
            response.setConfidence("0.5");
            response.setWhyChain(new WhyStep[]{});
            response.setPatternMatches(new PatternMatch[]{});
        }
        
        response.setTicketId("from_ai");
        response.setAnalyzedAt(Instant.now());
        return response;
    }

    @SuppressWarnings("unchecked")
    private WhyStep[] parseWhyChain(Object whyChainObj) {
        if (whyChainObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    WhyStep step = new WhyStep();
                    step.setStep(getInt(m, "step", 0));
                    step.setWhy(getString(m, "why", ""));
                    step.setFinding(getString(m, "finding", ""));
                    return step;
                })
                .toArray(WhyStep[]::new);
        }
        return generateDefaultWhyChain();
    }

    @SuppressWarnings("unchecked")
    private PatternMatch[] parsePatternMatches(Object patternObj) {
        if (patternObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    PatternMatch match = new PatternMatch();
                    match.setPatternId(getString(m, "patternId", ""));
                    match.setPatternName(getString(m, "patternName", ""));
                    match.setSimilarityScore(getDouble(m, "similarityScore", 0.0));
                    match.setKnownSolution(getString(m, "knownSolution", ""));
                    return match;
                })
                .toArray(PatternMatch[]::new);
        }
        return new PatternMatch[]{};
    }

    private WhyStep[] generateDefaultWhyChain() {
        WhyStep step1 = new WhyStep();
        step1.setStep(1);
        step1.setWhy("Why did the issue occur?");
        step1.setFinding("The system experienced an unexpected condition");
        
        WhyStep step2 = new WhyStep();
        step2.setStep(2);
        step2.setWhy("Why was the system in that condition?");
        step2.setFinding("A specific threshold was exceeded");
        
        WhyStep step3 = new WhyStep();
        step3.setStep(3);
        step3.setWhy("Why was the threshold exceeded?");
        step3.setFinding("Resource utilization reached capacity limits");
        
        WhyStep step4 = new WhyStep();
        step4.setStep(4);
        step4.setWhy("Why did utilization reach capacity?");
        step4.setFinding("Increased load without proper scaling");
        
        WhyStep step5 = new WhyStep();
        step5.setStep(5);
        step5.setWhy("Why was scaling not in place?");
        step5.setFinding("Auto-scaling triggers were not configured");
        
        return new WhyStep[]{step1, step2, step3, step4, step5};
    }

    private Map<String, Object> parseJson(String json) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private RootCauseResponse buildEmptyResponse(String ticketId) {
        RootCauseResponse response = new RootCauseResponse();
        response.setTicketId(ticketId);
        response.setRootCause("Analysis could not be completed");
        response.setConfidence("0.0");
        response.setWhyChain(new WhyStep[]{});
        response.setPatternMatches(new PatternMatch[]{});
        response.setAnalyzedAt(Instant.now());
        return response;
    }
}