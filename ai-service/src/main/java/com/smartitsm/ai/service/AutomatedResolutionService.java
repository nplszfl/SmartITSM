package com.smartitsm.ai.service;

import com.smartitsm.ai.client.DeepSeekClient;
import com.smartitsm.ai.dto.ResolutionSuggestRequest;
import com.smartitsm.ai.dto.ResolutionSuggestResponse;
import com.smartitsm.ai.dto.ResolutionSuggestResponse.SimilarCase;
import com.smartitsm.ai.dto.ResolutionSuggestResponse.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class AutomatedResolutionService {

    private static final Logger log = LoggerFactory.getLogger(AutomatedResolutionService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert IT resolution assistant. Given a ticket description, suggest or automate resolutions.
        
        Return JSON with:
        - suggestedResolution: comprehensive description of the resolution
        - confidence: confidence score (0.0-1.0)
        - autoExecutable: boolean indicating if this can be automated
        - steps: array of {order (int), description, command (optional), target (optional)}
        - requiredPermissions: array of permission strings needed
        - estimatedEffortMinutes: estimated time to implement
        - similarCases: array of {caseId, title, resolution, similarity (0.0-1.0)}
        
        Respond ONLY with valid JSON.
        """;

    private final DeepSeekClient deepSeekClient;

    public AutomatedResolutionService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Mono<ResolutionSuggestResponse> suggestResolution(ResolutionSuggestRequest request) {
        log.info("Suggesting resolution for ticket: {}", request.getTicketId());

        String userMessage = buildUserMessage(request);

        return deepSeekClient.chat(SYSTEM_PROMPT, userMessage)
            .map(this::parseAndBuildResponse)
            .defaultIfEmpty(buildDefaultResponse(request.getTicketId()))
            .doOnSuccess(response -> log.info("Resolution suggested for {}: confidence={}, autoExecutable={}",
                request.getTicketId(), response.getConfidence(), response.isAutoExecutable()))
            .doOnError(e -> log.error("Resolution suggestion failed for {}: {}",
                request.getTicketId(), e.getMessage()));
    }

    private String buildUserMessage(ResolutionSuggestRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket ID: ").append(request.getTicketId()).append("\n");
        sb.append("Description: ").append(request.getDescription()).append("\n");
        if (request.getCategory() != null) {
            sb.append("Category: ").append(request.getCategory()).append("\n");
        }
        if (request.getSubcategory() != null) {
            sb.append("Subcategory: ").append(request.getSubcategory()).append("\n");
        }
        if (request.getPreviousResolutions() != null && request.getPreviousResolutions().length > 0) {
            sb.append("Previous Resolutions Attempted: ").append(String.join(", ", request.getPreviousResolutions())).append("\n");
        }
        return sb.toString();
    }

    private ResolutionSuggestResponse parseAndBuildResponse(String aiResponse) {
        ResolutionSuggestResponse response = new ResolutionSuggestResponse();
        
        try {
            Map<String, Object> parsed = parseJson(aiResponse);
            
            response.setSuggestedResolution(getString(parsed, "suggestedResolution", "Manual intervention required"));
            response.setConfidence(getDouble(parsed, "confidence", 0.5));
            response.setAutoExecutable(getBool(parsed, "autoExecutable", false));
            response.setRequiredPermissions(parseStringArray(parsed.get("requiredPermissions")));
            response.setEstimatedEffortMinutes(getLong(parsed, "estimatedEffortMinutes", 30L));
            
            response.setSteps(parseSteps(parsed.get("steps")));
            response.setSimilarCases(parseSimilarCases(parsed.get("similarCases")));
            
        } catch (Exception e) {
            log.warn("Failed to parse resolution response: {}", e.getMessage());
            response = buildDefaultResponse("parse_error");
        }
        
        response.setGeneratedAt(Instant.now());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Step[] parseSteps(Object stepsObj) {
        if (stepsObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    Step step = new Step();
                    step.setOrder(getInt(m, "order", 0));
                    step.setDescription(getString(m, "description", ""));
                    step.setCommand(getString(m, "command", null));
                    step.setTarget(getString(m, "target", null));
                    return step;
                })
                .toArray(Step[]::new);
        }
        return new Step[]{};
    }

    @SuppressWarnings("unchecked")
    private SimilarCase[] parseSimilarCases(Object casesObj) {
        if (casesObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    SimilarCase c = new SimilarCase();
                    c.setCaseId(getString(m, "caseId", ""));
                    c.setTitle(getString(m, "title", ""));
                    c.setResolution(getString(m, "resolution", ""));
                    c.setSimilarity(getDouble(m, "similarity", 0.0));
                    return c;
                })
                .toArray(SimilarCase[]::new);
        }
        return new SimilarCase[]{};
    }

    private String[] parseStringArray(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream()
                .map(Object::toString)
                .toArray(String[]::new);
        }
        return new String[]{};
    }

    private ResolutionSuggestResponse buildDefaultResponse(String ticketId) {
        ResolutionSuggestResponse response = new ResolutionSuggestResponse();
        response.setTicketId(ticketId);
        response.setSuggestedResolution("Review ticket manually - could not generate automatic resolution");
        response.setConfidence(0.0);
        response.setAutoExecutable(false);
        response.setSteps(new Step[]{});
        response.setRequiredPermissions(new String[]{});
        response.setEstimatedEffortMinutes(0);
        response.setSimilarCases(new SimilarCase[]{});
        response.setGeneratedAt(Instant.now());
        return response;
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

    private boolean getBool(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
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
}