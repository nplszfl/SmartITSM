package com.smartitsm.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.common.ai.AIClient;
import com.smartitsm.common.dto.AIScoreResult;
import com.smartitsm.common.exception.BusinessException;
import com.smartitsm.ticket.dto.TicketCreateDTO;
import com.smartitsm.ticket.dto.TicketUpdateDTO;
import com.smartitsm.ticket.entity.Ticket;
import com.smartitsm.ticket.entity.TicketComment;
import com.smartitsm.ticket.entity.TicketHistory;
import com.smartitsm.ticket.repository.TicketRepository;
import com.smartitsm.ticket.repository.TicketCommentRepository;
import com.smartitsm.ticket.repository.TicketHistoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ticket Service - core ITSM service with AI integration.
 * Handles ticket lifecycle management with intelligent routing and prioritization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final AIClient aiClient;

    // Counter for ticket numbering
    private final AtomicLong ticketSequence = new AtomicLong(System.currentTimeMillis() % 10000);

    /**
     * Create a new ticket with AI-powered classification, prioritization, and routing.
     */
    @Transactional
    public Ticket createTicket(TicketCreateDTO dto) {
        log.info("Creating ticket: {}", dto.getTitle());

        // Generate unique ticket number
        String ticketNumber = generateTicketNumber();

        // Build ticket entity
        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : "SERVICE_REQUEST")
                .subCategory(dto.getSubCategory())
                .item(dto.getItem())
                .priority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM")
                .urgency(dto.getUrgency() != null ? dto.getUrgency() : "MEDIUM")
                .impact(dto.getImpact() != null ? dto.getImpact() : "MEDIUM")
                .status("NEW")
                .requesterId(dto.getRequesterId())
                .requesterName(dto.getRequesterName())
                .requesterEmail(dto.getRequesterEmail())
                .requesterDepartment(dto.getRequesterDepartment())
                .requesterPriority("NORMAL")
                .source(dto.getSource() != null ? dto.getSource() : "PORTAL")
                .affectedUsers(dto.getAffectedUsers())
                .assetId(dto.getAssetId())
                .build();

        // Apply AI Intelligence
        applyAIIntelligence(ticket);

        // Determine SLA based on impact/urgency
        calculateSLA(ticket);

        // Save ticket
        ticketRepository.save(ticket);
        log.info("Created ticket {} with AI score {}", ticketNumber, ticket.getAiScore());

        return ticket;
    }

    /**
     * Apply AI intelligence to ticket: scoring, classification, assignment suggestion.
     */
    private void applyAIIntelligence(Ticket ticket) {
        try {
            // Build AI scoring context
            AIClient.TicketScoringContext scoringContext = AIClient.TicketScoringContext.builder()
                    .title(ticket.getTitle())
                    .description(ticket.getDescription())
                    .category(ticket.getCategory())
                    .urgency(ticket.getUrgency())
                    .impact(ticket.getImpact())
                    .requesterName(ticket.getRequesterName())
                    .requesterPriority(ticket.getRequesterPriority())
                    .assignmentGroup(ticket.getAssignedGroup())
                    .affectedUsers(ticket.getAffectedUsers())
                    .slaTier(ticket.getSlaTier())
                    .avgResolutionHours(8.0) // Default
                    .groupBacklog(10) // Default
                    .escalationRate(15.0) // Default
                    .build();

            // Get AI score and priority recommendation
            AIScoreResult scoreResult = aiClient.scoreTicket(scoringContext);

            if (scoreResult != null) {
                ticket.setAiScore(scoreResult.getScore());
                ticket.setAiConfidence(scoreResult.getConfidence());
                ticket.setAiReasoning(scoreResult.getReasoning());
                ticket.setAiRecommendedAction(scoreResult.getRecommendedAction());

                // Auto-adjust priority based on AI score
                if (scoreResult.getScore() >= 80) {
                    ticket.setPriority("CRITICAL");
                } else if (scoreResult.getScore() >= 60) {
                    ticket.setPriority("HIGH");
                } else if (scoreResult.getScore() >= 40) {
                    ticket.setPriority("MEDIUM");
                } else {
                    ticket.setPriority("LOW");
                }

                log.info("AI scored ticket {} as {} with confidence {}", 
                    ticket.getTicketNumber(), scoreResult.getScore(), scoreResult.getConfidence());
            }

            // Get AI classification if not specified
            if (ticket.getCategory() == null || ticket.getCategory().equals("SERVICE_REQUEST")) {
                AIClient.TicketClassificationContext classContext = AIClient.TicketClassificationContext.builder()
                        .title(ticket.getTitle())
                        .description(ticket.getDescription())
                        .keywords(List.of("password", "access", "request"))
                        .build();

                String classification = aiClient.classifyTicket(classContext);
                if (classification != null && !classification.isEmpty()) {
                    ticket.setCategory(classification);
                }
            }

            // Get resolution time prediction
            AIClient.TicketPredictionContext predContext = AIClient.TicketPredictionContext.builder()
                    .title(ticket.getTitle())
                    .description(ticket.getDescription())
                    .category(ticket.getCategory())
                    .priority(ticket.getPriority())
                    .complexity("MEDIUM")
                    .urgency(ticket.getUrgency())
                    .build();

            Integer predictedHours = aiClient.predictResolutionTime(predContext);
            ticket.setPredictedResolutionHours(predictedHours);

            // Get assignment suggestion
            AIClient.TicketAssignmentContext assignContext = AIClient.TicketAssignmentContext.builder()
                    .title(ticket.getTitle())
                    .category(ticket.getCategory())
                    .complexity("MEDIUM")
                    .priorityScore(ticket.getAiScore() != null ? ticket.getAiScore().intValue() : 50)
                    .availableAgents(List.of(
                            Map.of("name", "agent1", "currentWorkload", 5, "avgResolutionHours", 4.0, "successRate", 0.95),
                            Map.of("name", "agent2", "currentWorkload", 3, "avgResolutionHours", 3.5, "successRate", 0.92)
                    ))
                    .build();

            AIClient.AssignmentSuggestion suggestion = aiClient.suggestAssignment(assignContext);
            ticket.setSuggestedAssignee(suggestion.getRecommendedAgent());

        } catch (Exception e) {
            log.error("AI intelligence application failed for ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            // Continue without AI - don't fail ticket creation
        }
    }

    /**
     * Calculate SLA deadlines based on priority.
     */
    private void calculateSLA(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();

        // Determine SLA tier based on priority matrix
        String priority = ticket.getPriority();
        String urgency = ticket.getUrgency();
        String impact = ticket.getImpact();

        int responseHours;
        int resolutionHours;

        if ("CRITICAL".equals(priority) || "CRITICAL".equals(urgency)) {
            ticket.setSlaTier("P1");
            responseHours = 1;
            resolutionHours = 4;
        } else if ("HIGH".equals(priority) || "HIGH".equals(urgency)) {
            ticket.setSlaTier("P2");
            responseHours = 2;
            resolutionHours = 8;
        } else if ("MEDIUM".equals(priority)) {
            ticket.setSlaTier("P3");
            responseHours = 8;
            resolutionHours = 24;
        } else {
            ticket.setSlaTier("P4");
            responseHours = 24;
            resolutionHours = 72;
        }

        // Adjust for impact
        if ("HIGH".equals(impact)) {
            resolutionHours = (int) (resolutionHours * 0.75);
        }

        ticket.setFirstResponseDue(now.plusHours(responseHours));
        ticket.setResolutionDue(now.plusHours(resolutionHours));
    }

    /**
     * Update ticket status and process state transitions.
     */
    @Transactional
    public Ticket updateTicketStatus(Long ticketId, String newStatus, String notes) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticket.setResolutionNotes(notes);

        // Track timestamps
        if ("IN_PROGRESS".equals(newStatus) && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(LocalDateTime.now());
        } else if ("RESOLVED".equals(newStatus)) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if ("CLOSED".equals(newStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticketRepository.updateById(ticket);

        log.info("Ticket {} status changed from {} to {}", ticket.getTicketNumber(), oldStatus, newStatus);

        // Check SLA compliance
        checkSLACompliance(ticket);

        return ticket;
    }

    /**
     * Assign ticket to agent or group.
     */
    @Transactional
    public Ticket assignTicket(Long ticketId, String assignee, String assignedGroup, String reason) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        ticket.setAssignedTo(assignee);
        ticket.setAssignedGroup(assignedGroup);
        ticket.setAssignmentReason(reason);

        if ("NEW".equals(ticket.getStatus())) {
            ticket.setStatus("OPEN");
        }

        ticketRepository.updateById(ticket);

        log.info("Ticket {} assigned to {} in group {}", ticket.getTicketNumber(), assignee, assignedGroup);

        return ticket;
    }

    /**
     * Get ticket with AI insights.
     */
    public Ticket getTicketWithAIInsights(Long ticketId) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        // Enhance with real-time AI insights
        if (ticket.getAiScore() == null || ticket.getAiScore() < 50) {
            // Re-score if previously low priority
            AIClient.TicketScoringContext context = AIClient.TicketScoringContext.builder()
                    .title(ticket.getTitle())
                    .description(ticket.getDescription())
                    .category(ticket.getCategory())
                    .urgency(ticket.getUrgency())
                    .impact(ticket.getImpact())
                    .requesterName(ticket.getRequesterName())
                    .requesterPriority(ticket.getRequesterPriority())
                    .affectedUsers(ticket.getAffectedUsers())
                    .build();

            AIScoreResult result = aiClient.scoreTicket(context);
            if (result != null) {
                ticket.setAiScore(result.getScore());
                ticket.setAiConfidence(result.getConfidence());
                ticket.setAiReasoning(result.getReasoning());
            }
        }

        return ticket;
    }

    /**
     * Search tickets with filters.
     */
    public IPage<Ticket> searchTickets(String status, String priority, String category,
                                       String assignedTo, String requesterId, int page, int size) {
        QueryWrapper<Ticket> query = new QueryWrapper<>();

        if (status != null) query.eq("status", status);
        if (priority != null) query.eq("priority", priority);
        if (category != null) query.eq("category", category);
        if (assignedTo != null) query.eq("assigned_to", assignedTo);
        if (requesterId != null) query.eq("requester_id", requesterId);

        query.orderByDesc("created_at");

        return ticketRepository.page(new Page<>(page, size), query);
    }

    /**
     * Get tickets requiring immediate attention (high AI score).
     */
    public List<Ticket> getUrgentTickets(int minScore) {
        QueryWrapper<Ticket> query = new QueryWrapper<>();
        query.ge("ai_score", minScore)
             .in("status", List.of("NEW", "OPEN"))
             .orderByDesc("ai_score")
             .last("LIMIT 50");
        return ticketRepository.list(query);
    }

    /**
     * Check SLA compliance and flag breaches.
     */
    private void checkSLACompliance(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();

        if (ticket.getFirstResponseDue() != null && now.isAfter(ticket.getFirstResponseDue())
                && ticket.getFirstResponseAt() == null) {
            log.warn("Ticket {} has breached first response SLA!", ticket.getTicketNumber());
        }

        if (ticket.getResolutionDue() != null && now.isAfter(ticket.getResolutionDue())
                && !"RESOLVED".equals(ticket.getStatus()) && !"CLOSED".equals(ticket.getStatus())) {
            log.warn("Ticket {} has breached resolution SLA!", ticket.getTicketNumber());
        }
    }

    /**
     * Generate unique ticket number.
     */
    private String generateTicketNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = ticketSequence.incrementAndGet();
        return String.format("TKT-%s-%04d", date, seq % 10000);
    }

    /**
     * Add a comment to a ticket.
     */
    @Transactional
    public TicketComment addComment(Long ticketId, String content, String authorId, 
                                    String authorName, String authorType, String visibility) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        TicketComment comment = TicketComment.builder()
                .ticketId(ticketId)
                .content(content)
                .authorId(authorId)
                .authorName(authorName)
                .authorType(authorType != null ? authorType : "AGENT")
                .visibility(visibility != null ? visibility : "EXTERNAL")
                .build();

        ticketCommentRepository.save(comment);

        // Record history
        recordHistory(ticketId, ticket.getTicketNumber(), "COMMENT", "Added comment", 
                      null, content, authorId, authorName, "Comment added");

        log.info("Comment added to ticket {} by {}", ticket.getTicketNumber(), authorName);
        return comment;
    }

    /**
     * Get all comments for a ticket.
     */
    public List<TicketComment> getComments(Long ticketId) {
        QueryWrapper<TicketComment> query = new QueryWrapper<>();
        query.eq("ticket_id", ticketId)
              .orderByAsc("created_at");
        return ticketCommentRepository.list(query);
    }

    /**
     * Record a history entry for ticket changes.
     */
    @Transactional
    public TicketHistory recordHistory(Long ticketId, String ticketNumber, String changeType,
                                        String fieldName, String oldValue, String newValue,
                                        String changedBy, String changedByName, String changeReason) {
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticketId)
                .ticketNumber(ticketNumber)
                .changeType(changeType)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .changedByName(changedByName)
                .changeReason(changeReason)
                .build();

        ticketHistoryRepository.save(history);
        log.debug("History recorded for ticket {} - {} changed by {}", 
                  ticketNumber, fieldName, changedBy);
        return history;
    }

    /**
     * Full update of a ticket (PUT).
     */
    @Transactional
    public Ticket updateTicket(Long ticketId, TicketUpdateDTO dto) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        // Update fields if provided
        if (dto.getTitle() != null) ticket.setTitle(dto.getTitle());
        if (dto.getDescription() != null) ticket.setDescription(dto.getDescription());
        if (dto.getCategory() != null) ticket.setCategory(dto.getCategory());
        if (dto.getSubCategory() != null) ticket.setSubCategory(dto.getSubCategory());
        if (dto.getItem() != null) ticket.setItem(dto.getItem());
        if (dto.getStatus() != null) ticket.setStatus(dto.getStatus());
        if (dto.getPriority() != null) ticket.setPriority(dto.getPriority());
        if (dto.getUrgency() != null) ticket.setUrgency(dto.getUrgency());
        if (dto.getImpact() != null) ticket.setImpact(dto.getImpact());
        if (dto.getAssignedTo() != null) ticket.setAssignedTo(dto.getAssignedTo());
        if (dto.getAssignedGroup() != null) ticket.setAssignedGroup(dto.getAssignedGroup());
        if (dto.getAssignmentReason() != null) ticket.setAssignmentReason(dto.getAssignmentReason());
        if (dto.getRequesterId() != null) ticket.setRequesterId(dto.getRequesterId());
        if (dto.getRequesterName() != null) ticket.setRequesterName(dto.getRequesterName());
        if (dto.getRequesterEmail() != null) ticket.setRequesterEmail(dto.getRequesterEmail());
        if (dto.getRequesterDepartment() != null) ticket.setRequesterDepartment(dto.getRequesterDepartment());
        if (dto.getRequesterPriority() != null) ticket.setRequesterPriority(dto.getRequesterPriority());
        if (dto.getSlaTier() != null) ticket.setSlaTier(dto.getSlaTier());
        if (dto.getAssetId() != null) ticket.setAssetId(dto.getAssetId());
        if (dto.getAssetName() != null) ticket.setAssetName(dto.getAssetName());
        if (dto.getWorkflowInstanceId() != null) ticket.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        if (dto.getCurrentWorkflowStep() != null) ticket.setCurrentWorkflowStep(dto.getCurrentWorkflowStep());
        if (dto.getAffectedUsers() != null) ticket.setAffectedUsers(dto.getAffectedUsers());
        if (dto.getBusinessValue() != null) ticket.setBusinessValue(dto.getBusinessValue());
        if (dto.getDowntimeImpact() != null) ticket.setDowntimeImpact(dto.getDowntimeImpact());
        if (dto.getLocation() != null) ticket.setLocation(dto.getLocation());
        if (dto.getResolutionCode() != null) ticket.setResolutionCode(dto.getResolutionCode());
        if (dto.getResolutionNotes() != null) ticket.setResolutionNotes(dto.getResolutionNotes());
        if (dto.getClosureNotes() != null) ticket.setClosureNotes(dto.getClosureNotes());
        if (dto.getSource() != null) ticket.setSource(dto.getSource());
        if (dto.getChannel() != null) ticket.setChannel(dto.getChannel());
        if (dto.getExternalTicketId() != null) ticket.setExternalTicketId(dto.getExternalTicketId());

        ticketRepository.updateById(ticket);
        log.info("Ticket {} fully updated", ticket.getTicketNumber());
        return ticket;
    }

    /**
     * Delete a ticket by ID.
     */
    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }
        ticketRepository.removeById(ticketId);
        log.info("Ticket {} deleted", ticket.getTicketNumber());
    }

    /**
     * Submit satisfaction rating for a ticket.
     */
    @Transactional
    public Ticket submitSatisfaction(Long ticketId, Integer rating, String comment) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        if (rating < 1 || rating > 5) {
            throw new BusinessException("INVALID_RATING", "Rating must be between 1 and 5");
        }

        ticket.setSatisfactionRating(rating);
        ticket.setSatisfactionComment(comment);

        // Record satisfaction in history
        recordHistory(ticketId, ticket.getTicketNumber(), "SATISFACTION", "satisfactionRating",
                      null, String.valueOf(rating), ticket.getRequesterId(), ticket.getRequesterName(),
                      "Customer satisfaction submitted");

        ticketRepository.updateById(ticket);
        log.info("Satisfaction submitted for ticket {}: rating={}", ticket.getTicketNumber(), rating);

        return ticket;
    }

    /**
     * Get ticket statistics.
     */
    public TicketStats getTicketStats() {
        TicketStats stats = new TicketStats();
        stats.setTotalOpen(ticketRepository.count(new QueryWrapper<Ticket>().in("status", List.of("NEW", "OPEN", "IN_PROGRESS"))));
        stats.setCritical(ticketRepository.count(new QueryWrapper<Ticket>().eq("priority", "CRITICAL").in("status", List.of("NEW", "OPEN"))));
        stats.setUnassigned(ticketRepository.count(new QueryWrapper<Ticket>().isNull("assigned_to").eq("status", "NEW")));
        stats.setBreachedSLA(ticketRepository.count(new QueryWrapper<Ticket>()
                .lt("resolution_due", LocalDateTime.now())
                .notIn("status", List.of("RESOLVED", "CLOSED"))));
        return stats;
    }

    /**
     * Get ticket by ticket number (business identifier).
     */
    public Ticket getTicketByTicketNumber(String ticketNumber) {
        QueryWrapper<Ticket> query = new QueryWrapper<>();
        query.eq("ticket_number", ticketNumber);
        Ticket ticket = ticketRepository.getOne(query);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketNumber);
        }
        return ticket;
    }

    /**
     * Get all history entries for a ticket.
     */
    public List<TicketHistory> getTicketHistory(Long ticketId) {
        QueryWrapper<TicketHistory> query = new QueryWrapper<>();
        query.eq("ticket_id", ticketId).orderByDesc("created_at");
        return ticketHistoryRepository.list(query);
    }

    /**
     * Get history by ticket number.
     */
    public List<TicketHistory> getTicketHistoryByTicketNumber(String ticketNumber) {
        QueryWrapper<TicketHistory> query = new QueryWrapper<>();
        query.eq("ticket_number", ticketNumber).orderByDesc("created_at");
        return ticketHistoryRepository.list(query);
    }

    /**
     * Get a single comment by ID.
     */
    public TicketComment getCommentById(Long commentId) {
        TicketComment comment = ticketCommentRepository.getById(commentId);
        if (comment == null) {
            throw new BusinessException("COMMENT_NOT_FOUND", "Comment not found: " + commentId);
        }
        return comment;
    }

    /**
     * Update/Edit an existing comment.
     */
    @Transactional
    public TicketComment updateComment(Long commentId, String content, String authorId) {
        TicketComment comment = ticketCommentRepository.getById(commentId);
        if (comment == null) {
            throw new BusinessException("COMMENT_NOT_FOUND", "Comment not found: " + commentId);
        }

        String oldContent = comment.getContent();
        comment.setContent(content);
        ticketCommentRepository.updateById(comment);

        // Record history
        recordHistory(comment.getTicketId(), null, "COMMENT_UPDATE", "content",
                oldContent, content, authorId, null, "Comment edited");

        log.info("Comment {} updated", commentId);
        return comment;
    }

    /**
     * Delete a comment.
     */
    @Transactional
    public void deleteComment(Long commentId, String deletedBy) {
        TicketComment comment = ticketCommentRepository.getById(commentId);
        if (comment == null) {
            throw new BusinessException("COMMENT_NOT_FOUND", "Comment not found: " + commentId);
        }

        ticketCommentRepository.removeById(commentId);

        // Record history
        recordHistory(comment.getTicketId(), null, "COMMENT_DELETE", "commentId",
                String.valueOf(commentId), null, deletedBy, null, "Comment deleted");

        log.info("Comment {} deleted", commentId);
    }

    /**
     * Escalate ticket priority with SLA recalculation.
     */
    @Transactional
    public Ticket escalateTicket(Long ticketId, String newPriority, String escalationReason) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        String oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        // Recalculate SLA based on new priority
        calculateSLA(ticket);

        // Record escalation in history
        recordHistory(ticketId, ticket.getTicketNumber(), "ESCALATION", "priority",
                oldPriority, newPriority, "SYSTEM", "System", escalationReason);

        ticketRepository.updateById(ticket);

        log.info("Ticket {} escalated from {} to {}", ticket.getTicketNumber(), oldPriority, newPriority);

        return ticket;
    }

    /**
     * Reopen a resolved or closed ticket.
     */
    @Transactional
    public Ticket reopenTicket(Long ticketId, String reason) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        if (!"RESOLVED".equals(ticket.getStatus()) && !"CLOSED".equals(ticket.getStatus())) {
            throw new BusinessException("INVALID_STATE", "Only resolved or closed tickets can be reopened");
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus("OPEN");
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticket.setResolutionCode(null);
        ticket.setResolutionNotes(null);

        // Recalculate SLA
        calculateSLA(ticket);

        // Record in history
        recordHistory(ticketId, ticket.getTicketNumber(), "STATUS_CHANGE", "status",
                oldStatus, "OPEN", "SYSTEM", "System", reason != null ? reason : "Ticket reopened");

        ticketRepository.updateById(ticket);

        log.info("Ticket {} reopened from {}", ticket.getTicketNumber(), oldStatus);

        return ticket;
    }

    /**
     * Bulk update status for multiple tickets.
     */
    @Transactional
    public int bulkUpdateStatus(List<Long> ticketIds, String newStatus, String changedBy) {
        int count = 0;
        for (Long ticketId : ticketIds) {
            try {
                Ticket ticket = ticketRepository.getById(ticketId);
                if (ticket != null) {
                    String oldStatus = ticket.getStatus();
                    ticket.setStatus(newStatus);

                    // Track timestamps
                    if ("IN_PROGRESS".equals(newStatus) && ticket.getFirstResponseAt() == null) {
                        ticket.setFirstResponseAt(LocalDateTime.now());
                    } else if ("RESOLVED".equals(newStatus)) {
                        ticket.setResolvedAt(LocalDateTime.now());
                    } else if ("CLOSED".equals(newStatus)) {
                        ticket.setClosedAt(LocalDateTime.now());
                    }

                    ticketRepository.updateById(ticket);

                    recordHistory(ticketId, ticket.getTicketNumber(), "STATUS_CHANGE", "status",
                            oldStatus, newStatus, changedBy, changedBy, "Bulk status update");

                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to update ticket {} in bulk operation: {}", ticketId, e.getMessage());
            }
        }
        log.info("Bulk status update completed: {} tickets updated to {}", count, newStatus);
        return count;
    }

    /**
     * Get tickets assigned to a specific user.
     */
    public List<Ticket> getTicketsByAssignee(String assignee) {
        QueryWrapper<Ticket> query = new QueryWrapper<>();
        query.eq("assigned_to", assignee).orderByDesc("created_at");
        return ticketRepository.list(query);
    }

    /**
     * Get tickets for a specific requester.
     */
    public List<Ticket> getTicketsByRequester(String requesterId) {
        QueryWrapper<Ticket> query = new QueryWrapper<>();
        query.eq("requester_id", requesterId).orderByDesc("created_at");
        return ticketRepository.list(query);
    }

    /**
     * Get SLA countdown information for a ticket.
     */
    public SLACountdown getSLACountdown(Long ticketId) {
        Ticket ticket = ticketRepository.getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId);
        }

        SLACountdown countdown = new SLACountdown();
        countdown.setTicketNumber(ticket.getTicketNumber());
        countdown.setSlaTier(ticket.getSlaTier());
        countdown.setStatus(ticket.getStatus());

        LocalDateTime now = LocalDateTime.now();

        // First response countdown
        if (ticket.getFirstResponseDue() != null) {
            countdown.setFirstResponseDue(ticket.getFirstResponseDue());
            if (ticket.getFirstResponseAt() != null) {
                countdown.setFirstResponseMet(true);
                countdown.setFirstResponseRemaining(null);
            } else {
                countdown.setFirstResponseMet(false);
                long minutesRemaining = java.time.Duration.between(now, ticket.getFirstResponseDue()).toMinutes();
                countdown.setFirstResponseRemaining(minutesRemaining);
                countdown.setFirstResponseBreached(minutesRemaining < 0);
            }
        }

        // Resolution countdown
        if (ticket.getResolutionDue() != null) {
            countdown.setResolutionDue(ticket.getResolutionDue());
            if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
                countdown.setResolutionMet(true);
                countdown.setResolutionRemaining(null);
            } else {
                countdown.setResolutionMet(false);
                long minutesRemaining = java.time.Duration.between(now, ticket.getResolutionDue()).toMinutes();
                countdown.setResolutionRemaining(minutesRemaining);
                countdown.setResolutionBreached(minutesRemaining < 0);
            }
        }

        return countdown;
    }

    @Data
    public static class SLACountdown {
        private String ticketNumber;
        private String slaTier;
        private String status;
        private LocalDateTime firstResponseDue;
        private Long firstResponseRemaining;
        private boolean firstResponseMet;
        private boolean firstResponseBreached;
        private LocalDateTime resolutionDue;
        private Long resolutionRemaining;
        private boolean resolutionMet;
        private boolean resolutionBreached;
    }

    @Data
    public static class TicketStats {
        private long totalOpen;
        private long critical;
        private long unassigned;
        private long breachedSLA;
    }
}