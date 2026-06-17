package com.smartitsm.sla.service;

import com.smartitsm.sla.dto.SlaCountdownResponse;
import com.smartitsm.sla.dto.SlaMonitorRequest;
import com.smartitsm.sla.dto.SlaStatsResponse;
import com.smartitsm.sla.entity.SlaAlert;
import com.smartitsm.sla.entity.SlaCompliance;
import com.smartitsm.sla.repository.SlaAlertRepository;
import com.smartitsm.sla.repository.SlaComplianceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlaAlertService.
 */
@ExtendWith(MockitoExtension.class)
class SlaAlertServiceTest {

    @Mock
    private SlaAlertRepository slaAlertRepository;

    @Mock
    private SlaComplianceRepository slaComplianceRepository;

    @InjectMocks
    private SlaAlertService slaAlertService;

    private SlaMonitorRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = SlaMonitorRequest.builder()
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .slaType("RESOLUTION")
            .slaTier("P2")
            .resolutionDue(LocalDateTime.now().plusHours(4))
            .firstResponseDue(LocalDateTime.now().plusHours(1))
            .assignedTo("agent001")
            .assignedGroup("L2_Support")
            .ticketTitle("Server down - production issue")
            .ticketStatus("OPEN")
            .ticketPriority("HIGH")
            .build();
    }

    @Test
    void testMonitorSla_CreatesNewAlert() {
        // Arrange
        when(slaAlertRepository.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);
        when(slaAlertRepository.save(any(SlaAlert.class))).thenReturn(true);

        // Act
        SlaAlert result = slaAlertService.monitorSla(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTicketId());
        assertEquals("TKT-20240529-0001", result.getTicketNumber());
        assertEquals("RESOLUTION", result.getSlaType());
        assertEquals("P2", result.getSlaTier());
        assertNotNull(result.getRemainingSeconds());
        assertNotNull(result.getAlertLevel());
        assertEquals("NORMAL", result.getAlertStatus());
        verify(slaAlertRepository, times(1)).save(any(SlaAlert.class));
    }

    @Test
    void testMonitorSla_UpdatesExistingAlert() {
        // Arrange
        SlaAlert existingAlert = SlaAlert.builder()
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .slaType("RESOLUTION")
            .slaTier("P2")
            .alertStatus("PENDING")
            .remainingSeconds(14400L)
            .build();
        existingAlert.setId(1L);

        when(slaAlertRepository.getOne(any())).thenReturn(existingAlert);
        when(slaAlertRepository.updateById(any(SlaAlert.class))).thenReturn(true);

        // Act
        SlaAlert result = slaAlertService.monitorSla(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getAlertStatus());
        verify(slaAlertRepository, times(1)).updateById(any(SlaAlert.class));
    }

    @Test
    void testMonitorSla_BreachDetected() {
        // Arrange
        SlaMonitorRequest breachedRequest = SlaMonitorRequest.builder()
            .ticketId(2L)
            .ticketNumber("TKT-20240529-0002")
            .slaType("RESOLUTION")
            .slaTier("P1")
            .resolutionDue(LocalDateTime.now().minusMinutes(30))
            .firstResponseDue(LocalDateTime.now().minusHours(1))
            .assignedTo("agent001")
            .assignedGroup("L2_Support")
            .ticketTitle("Critical production issue")
            .ticketStatus("OPEN")
            .ticketPriority("CRITICAL")
            .build();

        when(slaAlertRepository.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);
        when(slaAlertRepository.save(any(SlaAlert.class))).thenReturn(true);

        // Act
        SlaAlert result = slaAlertService.monitorSla(breachedRequest);

        // Assert
        assertNotNull(result);
        assertEquals("BREACHED", result.getAlertLevel());
        assertTrue(result.getRemainingSeconds() < 0);
    }

    @Test
    void testGetSlaCountdown_Success() {
        // Arrange
        SlaAlert alert = SlaAlert.builder()
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .slaType("RESOLUTION")
            .slaTier("P2")
            .remainingSeconds(7200L)
            .remainingTimeDisplay("2h 0m 0s")
            .alertLevel("YELLOW")
            .alertStatus("PENDING")
            .slaDueAt(LocalDateTime.now().plusHours(2))
            .ticketTitle("Server issue")
            .ticketStatus("OPEN")
            .assignedTo("agent001")
            .assignedGroup("L2_Support")
            .build();
        alert.setId(1L);
        alert.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(slaAlertRepository.getOne(any())).thenReturn(alert);

        // Act
        SlaCountdownResponse result = slaAlertService.getSlaCountdown(1L, "RESOLUTION");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTicketId());
        assertEquals("TKT-20240529-0001", result.getTicketNumber());
        assertEquals("RESOLUTION", result.getSlaType());
        assertEquals("P2", result.getSlaTier());
        assertNotNull(result.getRemainingSeconds());
        assertNotNull(result.getRemainingTimeDisplay());
        assertNotNull(result.getProgressPercentage());
    }

    @Test
    void testGetSlaCountdown_NoData() {
        // Arrange
        when(slaAlertRepository.getOne(any())).thenReturn(null);

        // Act
        SlaCountdownResponse result = slaAlertService.getSlaCountdown(999L, "RESOLUTION");

        // Assert
        assertNotNull(result);
        assertEquals(999L, result.getTicketId());
        assertEquals("NO_SLA_DATA", result.getStatus());
    }

    @Test
    void testGetSlaStats_Empty() {
        // Arrange
        when(slaComplianceRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());

        // Act
        SlaStatsResponse result = slaAlertService.getSlaStats("DAILY", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalTickets());
        assertEquals(0L, result.getMetSla());
        assertEquals(0L, result.getBreachedSla());
        assertEquals(0.0, result.getOverallComplianceRate());
    }

    @Test
    void testGetSlaStats_WithData() {
        // Arrange
        List<SlaCompliance> compliances = Arrays.asList(
            SlaCompliance.builder().ticketId(1L).slaType("RESOLUTION").slaTier("P1").met(true).build(),
            SlaCompliance.builder().ticketId(2L).slaType("FIRST_RESPONSE").slaTier("P1").met(true).build(),
            SlaCompliance.builder().ticketId(3L).slaType("RESOLUTION").slaTier("P2").met(false).build()
        );

        when(slaComplianceRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(compliances);

        // Act
        SlaStatsResponse result = slaAlertService.getSlaStats("DAILY", "2024-05-01", "2024-05-29");

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getTotalTickets());
        assertEquals(2L, result.getMetSla());
        assertEquals(1L, result.getBreachedSla());
        assertTrue(result.getOverallComplianceRate() > 0);
    }

    @Test
    void testRecordCompliance_Met() {
        // Arrange
        LocalDateTime slaDue = LocalDateTime.now().plusHours(4);
        LocalDateTime completed = LocalDateTime.now().plusHours(3);

        when(slaComplianceRepository.save(any(SlaCompliance.class))).thenReturn(true);

        // Act
        SlaCompliance result = slaAlertService.recordCompliance(
            1L, "TKT-20240529-0001", "RESOLUTION", slaDue, completed, "P2", "HIGH"
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.getMet());
        assertEquals("DAILY", result.getPeriodType());
        verify(slaComplianceRepository, times(1)).save(any(SlaCompliance.class));
    }

    @Test
    void testRecordCompliance_Breached() {
        // Arrange
        LocalDateTime slaDue = LocalDateTime.now().plusHours(4);
        LocalDateTime completed = LocalDateTime.now().plusHours(5);

        when(slaComplianceRepository.save(any(SlaCompliance.class))).thenReturn(true);

        // Act
        SlaCompliance result = slaAlertService.recordCompliance(
            1L, "TKT-20240529-0001", "RESOLUTION", slaDue, completed, "P2", "HIGH"
        );

        // Assert
        assertNotNull(result);
        assertFalse(result.getMet());
        assertTrue(result.getOverageSeconds() > 0);
    }

    @Test
    void testAcknowledgeAlert() {
        // Arrange
        SlaAlert alert = SlaAlert.builder()
            .ticketNumber("TKT-20240529-0001")
            .alertStatus("SENT")
            .build();
        alert.setId(1L);

        when(slaAlertRepository.getById(1L)).thenReturn(alert);
        when(slaAlertRepository.updateById(any(SlaAlert.class))).thenReturn(true);

        // Act
        SlaAlert result = slaAlertService.acknowledgeAlert(1L);

        // Assert
        assertNotNull(result);
        assertEquals("ACKNOWLEDGED", result.getAlertStatus());
        verify(slaAlertRepository, times(1)).updateById(any(SlaAlert.class));
    }

    @Test
    void testSendAlert() {
        // Arrange
        SlaAlert alert = SlaAlert.builder()
            .ticketNumber("TKT-20240529-0001")
            .alertLevel("RED")
            .remainingSeconds(300L)
            .remainingTimeDisplay("5m 0s")
            .alertStatus("PENDING")
            .build();
        alert.setId(1L);

        when(slaAlertRepository.updateById(any(SlaAlert.class))).thenReturn(true);

        // Act
        slaAlertService.sendAlert(alert, "EMAIL");

        // Assert
        assertEquals("SENT", alert.getAlertStatus());
        assertEquals("EMAIL", alert.getNotificationChannel());
        assertNotNull(alert.getAlertSentAt());
        assertEquals(1, alert.getReminderCount());
        verify(slaAlertRepository, times(1)).updateById(any(SlaAlert.class));
    }

    @Test
    void testGetActiveAlerts() {
        // Arrange
        List<SlaAlert> alerts = Arrays.asList(
            SlaAlert.builder().ticketNumber("TKT-001").alertLevel("RED").alertStatus("SENT").build(),
            SlaAlert.builder().ticketNumber("TKT-002").alertLevel("ORANGE").alertStatus("PENDING").build()
        );
        alerts.get(0).setId(1L);
        alerts.get(1).setId(2L);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(alerts);

        // Act
        List<SlaAlert> result = slaAlertService.getActiveAlerts(null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetActiveAlerts_FilteredByLevel() {
        // Arrange
        List<SlaAlert> alerts = List.of(
            SlaAlert.builder().ticketNumber("TKT-001").alertLevel("RED").alertStatus("SENT").build()
        );
        alerts.get(0).setId(1L);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(alerts);

        // Act
        List<SlaAlert> result = slaAlertService.getActiveAlerts("RED");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("RED", result.get(0).getAlertLevel());
    }

    // ========== NEW TESTS FOR MISSING BUSINESS FUNCTIONS ==========

    @Test
    void testGetAlertsByTicket() {
        // Arrange
        SlaAlert alert1 = SlaAlert.builder().ticketId(1L).ticketNumber("TKT-001").alertLevel("RED").build();
        alert1.setId(1L);
        SlaAlert alert2 = SlaAlert.builder().ticketId(1L).ticketNumber("TKT-001").alertLevel("ORANGE").build();
        alert2.setId(2L);
        List<SlaAlert> alerts = List.of(alert1, alert2);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(alerts);

        // Act
        List<SlaAlert> result = slaAlertService.getAlertsByTicket(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetBreachedAlerts() {
        // Arrange
        SlaAlert alert = SlaAlert.builder().ticketNumber("TKT-001").breached(true).alertLevel("BREACHED").build();
        alert.setId(1L);
        List<SlaAlert> breachedAlerts = List.of(alert);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(breachedAlerts);

        // Act
        List<SlaAlert> result = slaAlertService.getBreachedAlerts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getBreached());
    }

    @Test
    void testResolveAlert() {
        // Arrange
        SlaAlert alert = SlaAlert.builder()
            .ticketNumber("TKT-001")
            .alertStatus("SENT")
            .build();
        alert.setId(1L);

        when(slaAlertRepository.getById(1L)).thenReturn(alert);
        when(slaAlertRepository.updateById(any(SlaAlert.class))).thenReturn(true);

        // Act
        SlaAlert result = slaAlertService.resolveAlert(1L);

        // Assert
        assertNotNull(result);
        assertEquals("RESOLVED", result.getAlertStatus());
    }

    @Test
    void testGetAlertSummary() {
        // Arrange
        when(slaAlertRepository.count(any())).thenReturn(5L);

        // Act
        Map<String, Long> summary = slaAlertService.getAlertSummary();

        // Assert
        assertNotNull(summary);
        assertEquals(5L, summary.size());
    }

    @Test
    void testBulkAcknowledgeByTier() {
        // Arrange
        SlaAlert alert1 = SlaAlert.builder().ticketNumber("TKT-001").slaTier("P1").alertStatus("SENT").build();
        alert1.setId(1L);
        SlaAlert alert2 = SlaAlert.builder().ticketNumber("TKT-002").slaTier("P1").alertStatus("PENDING").build();
        alert2.setId(2L);
        List<SlaAlert> p1Alerts = List.of(alert1, alert2);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(p1Alerts);
        when(slaAlertRepository.updateById(any(SlaAlert.class))).thenReturn(true);

        // Act
        int count = slaAlertService.bulkAcknowledgeByTier("P1", "manager@company.com");

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testClearResolvedAlerts() {
        // Arrange
        SlaAlert oldResolved = SlaAlert.builder()
            .ticketNumber("TKT-001")
            .alertStatus("RESOLVED")
            .build();
        oldResolved.setId(1L);
        List<SlaAlert> resolvedAlerts = List.of(oldResolved);

        when(slaAlertRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(resolvedAlerts);
        when(slaAlertRepository.removeById(any(java.io.Serializable.class))).thenReturn(true);

        // Act
        int count = slaAlertService.clearResolvedAlerts(30);

        // Assert
        assertEquals(1, count);
    }

    @Test
    void testEstimateResolutionTime() {
        // Arrange
        SlaAlert alert = SlaAlert.builder()
            .ticketId(1L)
            .ticketNumber("TKT-001")
            .slaType("RESOLUTION")
            .remainingSeconds(3600L)
            .slaDueAt(LocalDateTime.now().plusHours(1))
            .build();
        alert.setId(1L);
        alert.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(slaAlertRepository.getOne(any())).thenReturn(alert);

        // Act
        Double estimate = slaAlertService.estimateResolutionTime(1L, "RESOLUTION");

        // Assert
        assertNotNull(estimate);
        assertTrue(estimate > 0);
    }
}