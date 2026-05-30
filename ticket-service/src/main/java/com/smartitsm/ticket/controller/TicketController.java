package com.smartitsm.ticket.controller;

import com.smartitsm.common.dto.ApiResponse;
import com.smartitsm.ticket.dto.TicketCreateDTO;
import com.smartitsm.ticket.dto.TicketUpdateDTO;
import com.smartitsm.ticket.entity.Ticket;
import com.smartitsm.ticket.entity.TicketComment;
import com.smartitsm.ticket.entity.TicketHistory;
import com.smartitsm.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ticket REST API Controller.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Create a new ticket.
     */
    @PostMapping
    public ApiResponse<Ticket> createTicket(@RequestBody TicketCreateDTO dto) {
        Ticket ticket = ticketService.createTicket(dto);
        return ApiResponse.ok(ticket);
    }

    /**
     * Get ticket by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<Ticket> getTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicketWithAIInsights(id);
        return ApiResponse.ok(ticket);
    }

    /**
     * Update ticket status.
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Ticket> updateStatus(@PathVariable Long id, @RequestParam String status,
                                            @RequestParam(required = false) String notes) {
        Ticket ticket = ticketService.updateTicketStatus(id, status, notes);
        return ApiResponse.ok(ticket);
    }

    /**
     * Assign ticket.
     */
    @PatchMapping("/{id}/assign")
    public ApiResponse<Ticket> assignTicket(@PathVariable Long id,
                                            @RequestParam String assignee,
                                            @RequestParam(required = false) String assignedGroup,
                                            @RequestParam(required = false) String reason) {
        Ticket ticket = ticketService.assignTicket(id, assignee, assignedGroup, reason);
        return ApiResponse.ok(ticket);
    }

    /**
     * Search tickets.
     */
    @GetMapping
    public ApiResponse<List<Ticket>> searchTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String requesterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(ticketService.searchTickets(status, priority, category, assignedTo, requesterId, page, size).getRecords());
    }

    /**
     * Get urgent tickets.
     */
    @GetMapping("/urgent")
    public ApiResponse<List<Ticket>> getUrgentTickets(@RequestParam(defaultValue = "70") int minScore) {
        return ApiResponse.ok(ticketService.getUrgentTickets(minScore));
    }

    /**
     * Get ticket statistics.
     */
    @GetMapping("/stats")
    public ApiResponse<TicketService.TicketStats> getStats() {
        return ApiResponse.ok(ticketService.getTicketStats());
    }

    /**
     * Full update of a ticket (PUT).
     */
    @PutMapping("/{id}")
    public ApiResponse<Ticket> updateTicket(@PathVariable Long id, @RequestBody TicketUpdateDTO dto) {
        Ticket ticket = ticketService.updateTicket(id, dto);
        return ApiResponse.ok(ticket);
    }

    /**
     * Delete a ticket.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ApiResponse.ok(null);
    }

    /**
     * Submit satisfaction rating for a ticket.
     */
    @PostMapping("/{id}/satisfaction")
    public ApiResponse<Ticket> submitSatisfaction(@PathVariable Long id,
                                                   @RequestParam Integer rating,
                                                   @RequestParam(required = false) String comment) {
        Ticket ticket = ticketService.submitSatisfaction(id, rating, comment);
        return ApiResponse.ok(ticket);
    }

    // ==================== Ticket Comments ====================

    /**
     * Add a comment to a ticket.
     */
    @PostMapping("/{id}/comments")
    public ApiResponse<TicketComment> addComment(@PathVariable Long id,
                                                 @RequestParam String content,
                                                 @RequestParam String authorId,
                                                 @RequestParam String authorName,
                                                 @RequestParam(required = false) String authorType,
                                                 @RequestParam(required = false) String visibility) {
        TicketComment comment = ticketService.addComment(id, content, authorId, authorName, authorType, visibility);
        return ApiResponse.ok(comment);
    }

    /**
     * Get all comments for a ticket.
     */
    @GetMapping("/{id}/comments")
    public ApiResponse<List<TicketComment>> getComments(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.getComments(id));
    }

    // ==================== Ticket History ====================

    /**
     * Record a history entry for a ticket.
     */
    @PostMapping("/{id}/history")
    public ApiResponse<TicketHistory> recordHistory(@PathVariable Long id,
                                                     @RequestParam String changeType,
                                                     @RequestParam(required = false) String fieldName,
                                                     @RequestParam(required = false) String oldValue,
                                                     @RequestParam(required = false) String newValue,
                                                     @RequestParam String changedBy,
                                                     @RequestParam String changedByName,
                                                     @RequestParam(required = false) String changeReason) {
        Ticket ticket = ticketService.getTicketWithAIInsights(id);
        TicketHistory history = ticketService.recordHistory(id, ticket.getTicketNumber(),
                changeType, fieldName, oldValue, newValue, changedBy, changedByName, changeReason);
        return ApiResponse.ok(history);
    }

    // ==================== Additional Business Functions ====================

    /**
     * Get ticket by ticket number.
     */
    @GetMapping("/number/{ticketNumber}")
    public ApiResponse<Ticket> getTicketByNumber(@PathVariable String ticketNumber) {
        Ticket ticket = ticketService.getTicketByTicketNumber(ticketNumber);
        return ApiResponse.ok(ticket);
    }

    /**
     * Get ticket history by ID.
     */
    @GetMapping("/{id}/history")
    public ApiResponse<List<TicketHistory>> getTicketHistory(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.getTicketHistory(id));
    }

    /**
     * Get ticket history by ticket number.
     */
    @GetMapping("/number/{ticketNumber}/history")
    public ApiResponse<List<TicketHistory>> getTicketHistoryByNumber(@PathVariable String ticketNumber) {
        return ApiResponse.ok(ticketService.getTicketHistoryByTicketNumber(ticketNumber));
    }

    /**
     * Update a comment.
     */
    @PutMapping("/comments/{commentId}")
    public ApiResponse<TicketComment> updateComment(@PathVariable Long commentId,
                                                     @RequestParam String content,
                                                     @RequestParam String authorId) {
        return ApiResponse.ok(ticketService.updateComment(commentId, content, authorId));
    }

    /**
     * Delete a comment.
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId,
                                            @RequestParam(required = false) String deletedBy) {
        ticketService.deleteComment(commentId, deletedBy);
        return ApiResponse.ok(null);
    }

    /**
     * Escalate a ticket's priority.
     */
    @PostMapping("/{id}/escalate")
    public ApiResponse<Ticket> escalateTicket(@PathVariable Long id,
                                              @RequestParam String priority,
                                              @RequestParam(required = false) String reason) {
        return ApiResponse.ok(ticketService.escalateTicket(id, priority, reason));
    }

    /**
     * Reopen a resolved or closed ticket.
     */
    @PostMapping("/{id}/reopen")
    public ApiResponse<Ticket> reopenTicket(@PathVariable Long id,
                                             @RequestParam(required = false) String reason) {
        return ApiResponse.ok(ticketService.reopenTicket(id, reason));
    }

    /**
     * Bulk update ticket status.
     */
    @PostMapping("/bulk/status")
    public ApiResponse<Integer> bulkUpdateStatus(@RequestParam List<Long> ticketIds,
                                                 @RequestParam String status,
                                                 @RequestParam(required = false) String changedBy) {
        return ApiResponse.ok(ticketService.bulkUpdateStatus(ticketIds, status, changedBy));
    }

    /**
     * Get tickets by assignee.
     */
    @GetMapping("/assignee/{assignee}")
    public ApiResponse<List<Ticket>> getTicketsByAssignee(@PathVariable String assignee) {
        return ApiResponse.ok(ticketService.getTicketsByAssignee(assignee));
    }

    /**
     * Get tickets by requester.
     */
    @GetMapping("/requester/{requesterId}")
    public ApiResponse<List<Ticket>> getTicketsByRequester(@PathVariable String requesterId) {
        return ApiResponse.ok(ticketService.getTicketsByRequester(requesterId));
    }

    /**
     * Get SLA countdown for a ticket.
     */
    @GetMapping("/{id}/sla-countdown")
    public ApiResponse<TicketService.SLACountdown> getSLACountdown(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.getSLACountdown(id));
    }
}