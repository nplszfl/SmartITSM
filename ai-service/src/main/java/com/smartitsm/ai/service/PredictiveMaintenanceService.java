package com.smartitsm.ai.service;

import com.smartitsm.ai.client.DeepSeekClient;
import com.smartitsm.ai.dto.MaintenancePredictRequest;
import com.smartitsm.ai.dto.MaintenancePredictResponse;
import com.smartitsm.ai.dto.MaintenancePredictResponse.FailurePrediction;
import com.smartitsm.ai.dto.MaintenancePredictResponse.MaintenanceRecommendation;
import com.smartitsm.ai.dto.MaintenancePredictResponse.RiskFactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class PredictiveMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveMaintenanceService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert predictive maintenance AI for IT infrastructure.
        Analyze system metrics and historical data to predict failures before they occur.
        
        Return JSON with:
        - prediction: {failureLikely (boolean), probability (0.0-1.0), predictedFailureType, estimatedTimeToFailure, confidencePercent (0-100)}
        - riskFactors: array of {factor, contribution (0.0-1.0), severity (Low/Medium/High/Critical)}
        - recommendations: array of {action, priority, estimatedDuration, requiredResources}
        
        Respond ONLY with valid JSON.
        """;

    private final DeepSeekClient deepSeekClient;

    public PredictiveMaintenanceService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Mono<MaintenancePredictResponse> predictFailure(MaintenancePredictRequest request) {
        log.info("Predicting failure for system: {}", request.getSystemId());

        String userMessage = buildUserMessage(request);

        return deepSeekClient.chat(SYSTEM_PROMPT, userMessage)
            .map(this::parseAndBuildResponse)
            .defaultIfEmpty(buildDefaultResponse(request.getSystemId()))
            .doOnSuccess(response -> log.info("Failure prediction for {}: likely={}, probability={}",
                request.getSystemId(),
                response.getPrediction().isFailureLikely(),
                response.getPrediction().getProbability()))
            .doOnError(e -> log.error("Failure prediction failed for {}: {}",
                request.getSystemId(), e.getMessage()));
    }

    private String buildUserMessage(MaintenancePredictRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("System ID: ").append(request.getSystemId()).append("\n");
        if (request.getSystemType() != null) {
            sb.append("System Type: ").append(request.getSystemType()).append("\n");
        }
        
        if (request.getRecentMetrics() != null && request.getRecentMetrics().length > 0) {
            sb.append("Recent Metrics:\n");
            for (MaintenancePredictRequest.MetricData metric : request.getRecentMetrics()) {
                sb.append("  - ").append(metric.getMetricName())
                    .append(": ").append(metric.getValue())
                    .append(" ").append(metric.getUnit() != null ? metric.getUnit() : "")
                    .append(" at ").append(metric.getTimestamp())
                    .append("\n");
            }
        }
        
        if (request.getRecentIncidents() != null && request.getRecentIncidents().length > 0) {
            sb.append("Recent Incidents: ").append(String.join(", ", request.getRecentIncidents())).append("\n");
        }
        
        return sb.toString();
    }

    private MaintenancePredictResponse parseAndBuildResponse(String aiResponse) {
        MaintenancePredictResponse response = new MaintenancePredictResponse();
        
        try {
            Map<String, Object> parsed = parseJson(aiResponse);
            
            response.setPrediction(parsePrediction(parsed.get("prediction")));
            response.setRiskFactors(parseRiskFactors(parsed.get("riskFactors")));
            response.setRecommendations(parseRecommendations(parsed.get("recommendations")));
            response.setPredictedAt(Instant.now());
            
        } catch (Exception e) {
            log.warn("Failed to parse AI prediction response: {}", e.getMessage());
            response = buildDefaultResponse("unknown");
        }
        
        return response;
    }

    @SuppressWarnings("unchecked")
    private FailurePrediction parsePrediction(Object predictionObj) {
        FailurePrediction prediction = new FailurePrediction();
        
        if (predictionObj instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) predictionObj;
            prediction.setFailureLikely(getBool(m, "failureLikely", false));
            prediction.setProbability(getDouble(m, "probability", 0.0));
            prediction.setPredictedFailureType(getString(m, "predictedFailureType", "Unknown"));
            prediction.setEstimatedTimeToFailure(getString(m, "estimatedTimeToFailure", "Unknown"));
            prediction.setConfidencePercent(getInt(m, "confidencePercent", 0));
        } else {
            prediction.setFailureLikely(false);
            prediction.setProbability(0.0);
            prediction.setPredictedFailureType("Analysis pending");
            prediction.setEstimatedTimeToFailure("Unknown");
            prediction.setConfidencePercent(0);
        }
        
        return prediction;
    }

    @SuppressWarnings("unchecked")
    private RiskFactor[] parseRiskFactors(Object riskFactorsObj) {
        if (riskFactorsObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    RiskFactor rf = new RiskFactor();
                    rf.setFactor(getString(m, "factor", "Unknown"));
                    rf.setContribution(getDouble(m, "contribution", 0.0));
                    rf.setSeverity(getString(m, "severity", "Low"));
                    return rf;
                })
                .toArray(RiskFactor[]::new);
        }
        return generateDefaultRiskFactors();
    }

    @SuppressWarnings("unchecked")
    private MaintenanceRecommendation[] parseRecommendations(Object recsObj) {
        if (recsObj instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    MaintenanceRecommendation rec = new MaintenanceRecommendation();
                    rec.setAction(getString(m, "action", "Monitor system"));
                    rec.setPriority(getString(m, "priority", "Low"));
                    rec.setEstimatedDuration(getString(m, "estimatedDuration", "Unknown"));
                    if (m.get("requiredResources") instanceof List<?>) {
                        List<?> resources = (List<?>) m.get("requiredResources");
                        rec.setRequiredResources(resources.stream().map(Object::toString).toArray(String[]::new));
                    }
                    return rec;
                })
                .toArray(MaintenanceRecommendation[]::new);
        }
        return new MaintenanceRecommendation[]{};
    }

    private RiskFactor[] generateDefaultRiskFactors() {
        RiskFactor rf1 = new RiskFactor();
        rf1.setFactor("CPU Usage");
        rf1.setContribution(0.3);
        rf1.setSeverity("Medium");
        
        RiskFactor rf2 = new RiskFactor();
        rf2.setFactor("Memory Utilization");
        rf2.setContribution(0.25);
        rf2.setSeverity("Medium");
        
        RiskFactor rf3 = new RiskFactor();
        rf3.setFactor("Disk I/O");
        rf3.setContribution(0.2);
        rf3.setSeverity("Low");
        
        return new RiskFactor[]{rf1, rf2, rf3};
    }

    private MaintenancePredictResponse buildDefaultResponse(String systemId) {
        MaintenancePredictResponse response = new MaintenancePredictResponse();
        response.setSystemId(systemId);
        
        FailurePrediction prediction = new FailurePrediction();
        prediction.setFailureLikely(false);
        prediction.setProbability(0.0);
        prediction.setPredictedFailureType("None predicted");
        prediction.setEstimatedTimeToFailure("N/A");
        prediction.setConfidencePercent(50);
        response.setPrediction(prediction);
        
        response.setRiskFactors(new RiskFactor[]{});
        response.setRecommendations(new MaintenanceRecommendation[]{});
        response.setPredictedAt(Instant.now());
        
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

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}