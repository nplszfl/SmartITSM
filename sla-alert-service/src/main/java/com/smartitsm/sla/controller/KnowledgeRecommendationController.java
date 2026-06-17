package com.smartitsm.sla.controller;

import com.smartitsm.sla.dto.KnowledgeRecommendationDto;
import com.smartitsm.sla.dto.KnowledgeRecommendRequest;
import com.smartitsm.sla.service.KnowledgeRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for knowledge base recommendations.
 */
@RestController
@RequestMapping("/api/sla/knowledge")
@RequiredArgsConstructor
public class KnowledgeRecommendationController {

    private final KnowledgeRecommendationService recommendationService;

    /**
     * Recommend knowledge articles for a ticket.
     */
    @PostMapping("/recommend")
    public List<KnowledgeRecommendationDto> recommend(@RequestBody KnowledgeRecommendRequest request) {
        return recommendationService.recommend(request);
    }

    /**
     * List recommendations for a ticket.
     */
    @GetMapping("/ticket/{ticketId}")
    public List<KnowledgeRecommendationDto> getByTicket(@PathVariable Long ticketId) {
        return recommendationService.getByTicket(ticketId);
    }

    /**
     * Mark a recommendation as clicked by the user.
     */
    @PostMapping("/{id}/clicked")
    public KnowledgeRecommendationDto markClicked(@PathVariable Long id) {
        return recommendationService.markClicked(id);
    }

    /**
     * Mark a recommendation as helpful.
     */
    @PostMapping("/{id}/helpful")
    public KnowledgeRecommendationDto markHelpful(@PathVariable Long id) {
        return recommendationService.markHelpful(id);
    }
}