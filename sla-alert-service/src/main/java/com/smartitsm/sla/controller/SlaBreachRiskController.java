package com.smartitsm.sla.controller;

import com.smartitsm.sla.dto.SlaBreachPredictRequest;
import com.smartitsm.sla.dto.SlaBreachRiskDto;
import com.smartitsm.sla.entity.SlaBreachRisk;
import com.smartitsm.sla.service.SlaBreachPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for SLA breach risk prediction.
 */
@RestController
@RequestMapping("/api/sla/risk")
@RequiredArgsConstructor
public class SlaBreachRiskController {

    private final SlaBreachPredictionService predictionService;

    /**
     * Predict SLA breach risk for a ticket.
     */
    @PostMapping("/predict")
    public SlaBreachRiskDto predictRisk(@RequestBody SlaBreachPredictRequest request) {
        return predictionService.predictRisk(request);
    }

    /**
     * List high-risk tickets above the given threshold (default 0.7).
     */
    @GetMapping("/high")
    public List<SlaBreachRiskDto> getHighRisk(
            @RequestParam(defaultValue = "0.7") double threshold) {
        return predictionService.getHighRiskTickets(threshold);
    }

    /**
     * Record that a ticket actually breached SLA (used for back-fill /
     * model evaluation).
     */
    @PostMapping("/breach")
    public SlaBreachRisk recordBreach(@RequestParam Long ticketId) {
        return predictionService.recordActualBreach(ticketId);
    }
}