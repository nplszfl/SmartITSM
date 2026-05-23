package com.smartitsm.ticket.controller;

import com.smartitsm.common.dto.ApiResponse;
import com.smartitsm.ticket.dto.TicketCreateDTO;
import com.smartitsm.ticket.dto.TicketUpdateDTO;
import com.smartitsm.ticket.entity.Ticket;
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
}