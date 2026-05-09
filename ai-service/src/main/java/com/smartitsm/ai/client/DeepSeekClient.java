package com.smartitsm.ai.client;

import com.smartitsm.ai.config.DeepSeekProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final WebClient webClient;
    private final DeepSeekProperties properties;

    public DeepSeekClient(WebClient.Builder webClientBuilder, DeepSeekProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Mono<String> chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ),
            "temperature", 0.7,
            "max_tokens", 2000
        );

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message != null ? (String) message.get("content") : "";
                }
                return "";
            })
            .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofMillis(properties.getRetryDelayMs()))
                .filter(this::isRetryable)
                .doBeforeRetry(signal -> log.warn("Retrying DeepSeek API call, attempt {}: {}",
                    signal.totalRetries() + 1, signal.failure().getMessage())))
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("DeepSeek API call failed: {}", e.getMessage()));
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            return status.value() == 429 || status.value() >= 500;
        }
        return throwable instanceof java.net.ConnectException
            || throwable instanceof java.net.SocketTimeoutException;
    }
}