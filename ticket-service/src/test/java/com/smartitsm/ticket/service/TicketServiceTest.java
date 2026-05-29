package com.smartitsm.ticket.service;

import com.smartitsm.common.ai.AIClient;
import com.smartitsm.common.dto.AIScoreResult;
import com.smartitsm.common.exception.BusinessException;
import com.smartitsm.ticket.dto.TicketCreateDTO;
import com.smartitsm.ticket.dto.TicketUpdateDTO;
import com.smartitsm.ticket.entity.Ticket;
import com.smartitsm.ticket.entity.TicketComment;
import com.smartitsm.ticket.entity.TicketHistory;
import com.smartitsm.ticket.repository.TicketCommentRepository;
import com.smartitsm.ticket.repository.TicketHistoryRepository;
import com.smartitsm.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TicketService.
 * Tests ticket lifecycle, AI scoring, SLA calculation, and comment handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private TicketHistoryRepository ticketHistoryRepository;
    @Mock
    private AIClient aiClient;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository, ticketCommentRepository, ticketHistoryRepository, aiClient);
    }

    @Test
    void createTicket_withValidData_generatesTicketNumberAndSla() {
        // Arrange
        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("Cannot access email");
        dto.setDescription("Email application not loading");
        dto.setPriority("HIGH");
        dto.setRequesterId(1L);
        dto.setRequesterName("John Doe");
        dto.setRequesterEmail("john@company.com");

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        AIScoreResult scoreResult = new AIScoreResult();
        scoreResult.setScore(75.0);
        scoreResult.setConfidence("HIGH");
        scoreResult.setReasoning("High priority ticket detected");
        scoreResult.setRecommendedAction("ESCALATE");
        when(aiClient.scoreTicket(any())).thenReturn(scoreResult);
        when(aiClient.classifyTicket(any())).thenReturn("INCIDENT");
        when(aiClient.predictResolutionTime(any())).thenReturn(4);
        when(aiClient.suggestAssignment(any())).thenReturn(
                AIClient.AssignmentSuggestion.builder().recommendedAgent("agent1").build());

        // Act
        Ticket result = ticketService.createTicket(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Cannot access email");
        assertThat(result.getStatus()).isEqualTo("NEW");
        assertThat(result.getTicketNumber()).startsWith("TKT-");
        assertThat(result.getSlaTier()).isEqualTo("P2"); // HIGH priority -> P2
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicket_withCriticalPriority_setsP1Sla() {
        // Arrange
        TicketCreateDTO dto = new TicketCreateDTO();
        dto.setTitle("System down");
        dto.setDescription("Critical system outage");
        dto.setPriority("CRITICAL");
        dto.setRequesterId(1L);
        dto.setRequesterName("Admin");

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });
        
        AIScoreResult scoreResult = new AIScoreResult();
        scoreResult.setScore(95.0);
        scoreResult.setConfidence("HIGH");
        scoreResult.setRecommendedAction("IMMEDIATE");
        when(aiClient.scoreTicket(any())).thenReturn(scoreResult);
        when(aiClient.classifyTicket(any())).thenReturn("INCIDENT");
        when(aiClient.predictResolutionTime(any())).thenReturn(1);
        when(aiClient.suggestAssignment(any())).thenReturn(
                AIClient.AssignmentSuggestion.builder().recommendedAgent("agent1").build());

        // Act
        Ticket result = ticketService.createTicket(dto);

        // Assert
        assertThat(result.getSlaTier()).isEqualTo("P1");
        assertThat(result.getPriority()).isEqualTo("CRITICAL");
    }

    @Test
    void updateTicketStatus_toInProgress_setsFirstResponseTimestamp() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");
        ticket.setStatus("NEW");
        ticket.setFirstResponseAt(null);

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);
        when(ticketRepository.updateById(any(Ticket.class))).thenReturn(true);

        // Act
        Ticket result = ticketService.updateTicketStatus(ticketId, "IN_PROGRESS", "Working on it");

        // Assert
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.getFirstResponseAt()).isNotNull();
        verify(ticketRepository).updateById(any(Ticket.class));
    }

    @Test
    void updateTicketStatus_toResolved_setsResolvedTimestamp() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");
        ticket.setStatus("IN_PROGRESS");

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);
        when(ticketRepository.updateById(any(Ticket.class))).thenReturn(true);

        // Act
        Ticket result = ticketService.updateTicketStatus(ticketId, "RESOLVED", "Fixed the issue");

        // Assert
        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void updateTicketStatus_whenNotFound_throwsException() {
        // Arrange
        Long ticketId = 999L;
        when(ticketRepository.getById(ticketId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.updateTicketStatus(ticketId, "IN_PROGRESS", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TICKET_NOT_FOUND");
    }

    @Test
    void assignTicket_whenNew_setsStatusToOpen() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");
        ticket.setStatus("NEW");

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);
        when(ticketRepository.updateById(any(Ticket.class))).thenReturn(true);

        // Act
        Ticket result = ticketService.assignTicket(ticketId, "agent1", "IT Support", "Specialist");

        // Assert
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getAssignedTo()).isEqualTo("agent1");
        assertThat(result.getAssignedGroup()).isEqualTo("IT Support");
    }

    @Test
    void addComment_createsCommentAndHistory() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setId(1L);
            return comment;
        });
        when(ticketHistoryRepository.save(any(TicketHistory.class))).thenReturn(new TicketHistory());

        // Act
        TicketComment result = ticketService.addComment(
                ticketId, "Working on this issue", "agent1", "Agent Smith", "AGENT", "EXTERNAL");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Working on this issue");
        assertThat(result.getAuthorName()).isEqualTo("Agent Smith");
        verify(ticketCommentRepository).save(any(TicketComment.class));
        verify(ticketHistoryRepository).save(any(TicketHistory.class));
    }

    @Test
    void addComment_whenTicketNotFound_throwsException() {
        // Arrange
        Long ticketId = 999L;
        when(ticketRepository.getById(ticketId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.addComment(ticketId, "comment", "author", "Author", null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getComments_returnsCommentsOrderedByCreatedAt() {
        // Arrange
        Long ticketId = 1L;
        TicketComment comment1 = new TicketComment();
        comment1.setContent("First");
        TicketComment comment2 = new TicketComment();
        comment2.setContent("Second");

        when(ticketCommentRepository.list(any())).thenReturn(List.of(comment1, comment2));

        // Act
        List<TicketComment> results = ticketService.getComments(ticketId);

        // Assert
        assertThat(results).hasSize(2);
    }

    @Test
    void getUrgentTickets_returnsHighAiScoreTickets() {
        // Arrange
        Ticket ticket1 = new Ticket();
        ticket1.setAiScore(85.0);
        ticket1.setStatus("NEW");

        when(ticketRepository.list(any())).thenReturn(List.of(ticket1));

        // Act
        List<Ticket> results = ticketService.getUrgentTickets(80);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAiScore()).isEqualTo(85.0);
    }

    @Test
    void submitSatisfaction_withValidRating_updatesTicket() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);
        when(ticketRepository.updateById(any(Ticket.class))).thenReturn(true);
        when(ticketHistoryRepository.save(any(TicketHistory.class))).thenReturn(new TicketHistory());

        // Act
        Ticket result = ticketService.submitSatisfaction(ticketId, 5, "Great support!");

        // Assert
        assertThat(result.getSatisfactionRating()).isEqualTo(5);
        assertThat(result.getSatisfactionComment()).isEqualTo("Great support!");
    }

    @Test
    void submitSatisfaction_withInvalidRating_throwsException() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.submitSatisfaction(ticketId, 6, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_RATING");

        assertThatThrownBy(() -> ticketService.submitSatisfaction(ticketId, 0, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_RATING");
    }

    @Test
    void getTicketStats_returnsCorrectCounts() {
        // Arrange
        when(ticketRepository.count(any())).thenReturn(10L);

        // Act
        TicketService.TicketStats stats = ticketService.getTicketStats();

        // Assert
        assertThat(stats.getTotalOpen()).isEqualTo(10L);
        assertThat(stats.getCritical()).isEqualTo(10L);
        assertThat(stats.getUnassigned()).isEqualTo(10L);
        assertThat(stats.getBreachedSLA()).isEqualTo(10L);
    }

    @Test
    void deleteTicket_removesTicket() {
        // Arrange
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketNumber("TKT-20260101-0001");

        when(ticketRepository.getById(ticketId)).thenReturn(ticket);

        // Act
        ticketService.deleteTicket(ticketId);

        // Assert
        verify(ticketRepository).deleteById(ticketId);
    }

    @Test
    void deleteTicket_whenNotFound_throwsException() {
        // Arrange
        Long ticketId = 999L;
        when(ticketRepository.getById(ticketId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.deleteTicket(ticketId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void searchTickets_withFilters_returnsFilteredResults() {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setStatus("OPEN");
        ticket.setPriority("HIGH");

        when(ticketRepository.page(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.core.metadata.IPage<>() {
                    @Override
                    public List<Ticket> getRecords() { return List.of(ticket); }
                    @Override
                    public long getTotal() { return 1; }
                    @Override
                    public boolean hasNext() { return false; }
                    @Override
                    public boolean hasPrevious() { return false; }
                });

        // Act
        var result = ticketService.searchTickets("OPEN", "HIGH", null, null, null, 1, 10);

        // Assert
        assertThat(result.getRecords()).hasSize(1);
    }
}