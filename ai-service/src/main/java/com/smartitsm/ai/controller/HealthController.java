package com.smartitsm.ai.controller;

import com.smartitsm.ai.config.DeepSeekProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class HealthController {

    private final DeepSeekProperties deepSeekProperties;

    public HealthController(DeepSeekProperties deepSeekProperties) {
        this.deepSeekProperties = deepSeekProperties;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "service", "smartitsm-ai-service",
            "timestamp", Instant.now().toString(),
            "version", "1.0.0",
            "capabilities", Map.of(
                "ticketClassification", true,
                "rootCauseAnalysis", true,
                "predictiveMaintenance", true,
                "automatedResolution", true
            ),
            "llm", Map.of(
                "provider", "deepseek",
                "model", deepSeekProperties.getModel(),
                "status", "configured"
            )
        ));
    }
}