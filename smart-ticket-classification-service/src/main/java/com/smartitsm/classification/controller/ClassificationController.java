package com.smartitsm.classification.controller;

import com.smartitsm.classification.dto.ClassificationRequest;
import com.smartitsm.classification.dto.ClassificationResponse;
import com.smartitsm.classification.dto.ClassificationStats;
import com.smartitsm.classification.entity.TicketClassification;
import com.smartitsm.classification.service.TicketClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for ticket classification.
 */
@RestController
@RequestMapping("/api/v1/classifications")
@RequiredArgsConstructor
public class ClassificationController {

    private final TicketClassificationService classificationService;

    /**
     * Classify a ticket.
     */
    @PostMapping
    public ClassificationResponse classifyTicket(@RequestBody ClassificationRequest request) {
        return classificationService.classifyTicket(request);
    }

    /**
     * Get classification for a specific ticket.
     */
    @GetMapping("/ticket/{ticketId}")
    public ClassificationResponse getClassification(@PathVariable Long ticketId) {
        TicketClassification classification = classificationService.getClassificationByTicketId(ticketId);
        if (classification == null) {
            return ClassificationResponse.builder()
                .ticketId(ticketId)
                .status("NOT_FOUND")
                .message("No classification found for this ticket")
                .build();
        }

        return ClassificationResponse.builder()
            .ticketId(classification.getTicketId())
            .ticketNumber(classification.getTicketNumber())
            .category(classification.getCategory())
            .subCategory(classification.getSubCategory())
            .categoryConfidence(classification.getCategoryConfidence())
            .classificationMethod(classification.getClassificationMethod())
            .priority(classification.getPriority())
            .priorityScore(classification.getPriorityScore())
            .priorityReasoning(classification.getPriorityReasoning())
            .assignedGroup(classification.getAssignedGroup())
            .suggestedAssignee(classification.getSuggestedAssignee())
            .routingConfidence(classification.getRoutingConfidence())
            .status("SUCCESS")
            .build();
    }

    /**
     * Get classification statistics.
     */
    @GetMapping("/stats")
    public ClassificationStats getStats() {
        return classificationService.getClassificationStats();
    }

    /**
     * Batch classify multiple tickets.
     */
    @PostMapping("/batch")
    public List<ClassificationResponse> batchClassify(@RequestBody List<ClassificationRequest> requests) {
        return requests.stream()
            .map(classificationService::classifyTicket)
            .toList();
    }
}