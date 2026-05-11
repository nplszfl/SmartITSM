package com.smartitsm.ai.controller;

import com.smartitsm.ai.dto.TicketClassifyRequest;
import com.smartitsm.ai.dto.TicketClassifyResponse;
import com.smartitsm.ai.dto.RootCauseRequest;
import com.smartitsm.ai.dto.RootCauseResponse;
import com.smartitsm.ai.dto.MaintenancePredictRequest;
import com.smartitsm.ai.dto.MaintenancePredictResponse;
import com.smartitsm.ai.dto.ResolutionSuggestRequest;
import com.smartitsm.ai.dto.ResolutionSuggestResponse;
import com.smartitsm.ai.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiEndpointsController {

    private static final Logger log = LoggerFactory.getLogger(AiEndpointsController.class);
    private final TicketClassificationService ticketClassificationService;

    public AiEndpointsController(TicketClassificationService ticketClassificationService) {
        this.ticketClassificationService = ticketClassificationService;
    }

    @PostMapping(value = "/ticket/classify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TicketClassifyResponse>> classifyTicket(
            @Valid @RequestBody TicketClassifyRequest request) {
        log.info("POST /api/v1/ai/ticket/classify - ticketId: {}", request.getTicketId());
        
        return ticketClassificationService.classifyTicket(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(r -> log.info("Classified ticket {} with priority {}", 
                request.getTicketId(), r.getBody() != null ? r.getBody().getPriority() : "unknown"))
            .doOnError(e -> log.error("Failed to classify ticket {}: {}", request.getTicketId(), e.getMessage()))
            .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}

@RestController
@RequestMapping("/api/v1/ai")
class AiRootCauseController {

    private static final Logger log = LoggerFactory.getLogger(AiRootCauseController.class);
    private final RootCauseAnalysisService rootCauseAnalysisService;

    public AiRootCauseController(RootCauseAnalysisService rootCauseAnalysisService) {
        this.rootCauseAnalysisService = rootCauseAnalysisService;
    }

    @PostMapping(value = "/root-cause/analyze", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RootCauseResponse>> analyzeRootCause(
            @Valid @RequestBody RootCauseRequest request) {
        log.info("POST /api/v1/ai/root-cause/analyze - ticketId: {}", request.getTicketId());
        
        return rootCauseAnalysisService.analyze(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(r -> log.info("Analyzed root cause for {}: {}", 
                request.getTicketId(), r.getBody() != null ? r.getBody().getRootCause() : "unknown"))
            .doOnError(e -> log.error("Failed to analyze root cause for {}: {}", request.getTicketId(), e.getMessage()))
            .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}

@RestController
@RequestMapping("/api/v1/ai")
class AiMaintenanceController {

    private static final Logger log = LoggerFactory.getLogger(AiMaintenanceController.class);
    private final PredictiveMaintenanceService predictiveMaintenanceService;

    public AiMaintenanceController(PredictiveMaintenanceService predictiveMaintenanceService) {
        this.predictiveMaintenanceService = predictiveMaintenanceService;
    }

    @PostMapping(value = "/maintenance/predict-failure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MaintenancePredictResponse>> predictFailure(
            @Valid @RequestBody MaintenancePredictRequest request) {
        log.info("POST /api/v1/ai/maintenance/predict-failure - systemId: {}", request.getSystemId());
        
        return predictiveMaintenanceService.predictFailure(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(r -> log.info("Predicted failure for {}: likely={}", 
                request.getSystemId(), r.getBody() != null ? r.getBody().getPrediction().isFailureLikely() : false))
            .doOnError(e -> log.error("Failed to predict failure for {}: {}", request.getSystemId(), e.getMessage()))
            .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}

@RestController
@RequestMapping("/api/v1/ai")
class AiResolutionController {

    private static final Logger log = LoggerFactory.getLogger(AiResolutionController.class);
    private final AutomatedResolutionService automatedResolutionService;

    public AiResolutionController(AutomatedResolutionService automatedResolutionService) {
        this.automatedResolutionService = automatedResolutionService;
    }

    @PostMapping(value = "/resolution/suggest", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ResolutionSuggestResponse>> suggestResolution(
            @Valid @RequestBody ResolutionSuggestRequest request) {
        log.info("POST /api/v1/ai/resolution/suggest - ticketId: {}", request.getTicketId());
        
        return automatedResolutionService.suggestResolution(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(r -> log.info("Suggested resolution for {}: autoExecutable={}", 
                request.getTicketId(), r.getBody() != null ? r.getBody().isAutoExecutable() : false))
            .doOnError(e -> log.error("Failed to suggest resolution for {}: {}", request.getTicketId(), e.getMessage()))
            .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}