package com.smartitsm.ai.service;

import com.smartitsm.ai.client.DeepSeekClient;
import com.smartitsm.ai.dto.TicketClassifyRequest;
import com.smartitsm.ai.dto.TicketClassifyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TicketClassificationService {

    private static final Logger log = LoggerFactory.getLogger(TicketClassificationService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert IT Service Management (ITSM) AI assistant specializing in ticket classification.
        Analyze incoming support tickets and classify them accurately.
        
        Categories: Hardware, Software, Network, Security, Access, Application, Infrastructure, Other
        Priorities: Critical, High, Medium, Low
        Teams: L1 Support, L2 Support, L3 Support, Network Team, Security Team, DevOps, Database Team
        
        Return a JSON response with these fields:
        - category: primary category
        - subcategory: specific subcategory
        - priority: urgency-based priority level
        - urgency: time sensitivity
        - impact: business impact level
        - assignedTeam: appropriate team for handling
        - assignedTo: suggested assignee role (optional)
        - confidence: confidence score (0.0-1.0)
        - rationale: brief explanation of classification decision
        
        Respond ONLY with valid JSON, no additional text.
        """;

    private final DeepSeekClient deepSeekClient;

    public TicketClassificationService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Mono<TicketClassifyResponse> classifyTicket(TicketClassifyRequest request) {
        log.info("Classifying ticket: {}", request.getTicketId());

        String userMessage = buildUserMessage(request);

        return deepSeekClient.chat(SYSTEM_PROMPT, userMessage)
            .map(this::parseAndBuildResponse)
            .doOnSuccess(response -> log.info("Ticket {} classified as {} priority, team: {}",
                request.getTicketId(), response.getPriority(), response.getAssignedTeam()))
            .doOnError(e -> log.error("Failed to classify ticket {}: {}", request.getTicketId(), e.getMessage()));
    }

    private String buildUserMessage(TicketClassifyRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket ID: ").append(request.getTicketId()).append("\n");
        sb.append("Title: ").append(request.getTitle()).append("\n");
        sb.append("Description: ").append(request.getDescription()).append("\n");
        if (request.getReportedBy() != null) {
            sb.append("Reported By: ").append(request.getReportedBy()).append("\n");
        }
        if (request.getSource() != null) {
            sb.append("Source: ").append(request.getSource()).append("\n");
        }
        return sb.toString();
    }

    private TicketClassifyResponse parseAndBuildResponse(String aiResponse) {
        TicketClassifyResponse response = new TicketClassifyResponse();
        
        try {
            Map<String, Object> parsed = parseJson(aiResponse);
            
            response.setTicketId(parseTicketIdFromResponse(aiResponse));
            response.setCategory(getString(parsed, "category", "Other"));
            response.setSubcategory(getString(parsed, "subcategory", "General"));
            response.setPriority(getString(parsed, "priority", "Medium"));
            response.setUrgency(getString(parsed, "urgency", "Normal"));
            response.setImpact(getString(parsed, "impact", "Low"));
            response.setAssignedTeam(getString(parsed, "assignedTeam", "L1 Support"));
            response.setAssignedTo(getString(parsed, "assignedTo", null));
            response.setConfidence(getString(parsed, "confidence", "0.75"));
            response.setRationale(getString(parsed, "rationale", "AI classified based on content analysis"));
        } catch (Exception e) {
            log.warn("Failed to parse AI response, using fallback classification: {}", e.getMessage());
            response = fallbackClassification(response);
        }
        
        response.setProcessedAt(Instant.now());
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

    private String parseTicketIdFromResponse(String response) {
        return "extracted";
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private TicketClassifyResponse fallbackClassification(TicketClassifyResponse response) {
        response.setCategory("Other");
        response.setSubcategory("General");
        response.setPriority("Medium");
        response.setUrgency("Normal");
        response.setImpact("Low");
        response.setAssignedTeam("L1 Support");
        response.setConfidence("0.5");
        response.setRationale("Fallback classification due to parsing error");
        return response;
    }
}