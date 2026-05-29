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
        when(slaAlertRepository.getOne(any())).thenReturn(null);
        when(slaAlertRepository.save(any(SlaAlert.class)))
            .thenAnswer(invocation -> {
                SlaAlert alert = invocation.getArgument(0);
                alert.setId(1L);
                return alert;
            });

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
        assertEquals("PENDING", result.getAlertStatus());
        verify(slaAlertRepository, times(1)).save(any(SlaAlert.class));
    }

    @Test
    void testMonitorSla_UpdatesExistingAlert() {
        // Arrange
        SlaAlert existingAlert = SlaAlert.builder()
            .id(1L)
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .slaType("RESOLUTION")
            .slaTier("P2")
            .alertStatus("PENDING")
            .remainingSeconds(14400L)
            .build();

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
            .resolutionDue(LocalDateTime.now().minusMinutes(30))  // Already breached
            .firstResponseDue(LocalDateTime.now().minusHours(1))
            .assignedTo("agent001")
            .assignedGroup("L2_Support")
            .ticketTitle("Critical production issue")
            .ticketStatus("OPEN")
            .ticketPriority("CRITICAL")
            .build();

        when(slaAlertRepository.getOne(any())).thenReturn(null);
        when(slaAlertRepository.save(any(SlaAlert.class)))
            .thenAnswer(invocation -> {
                SlaAlert alert = invocation.getArgument(0);
                alert.setId(2L);
                return alert;
            });

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
            .id(1L)
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
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();

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
        when(slaComplianceRepository.list(any())).thenReturn(List.of());

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

        when(slaComplianceRepository.list(any())).thenReturn(compliances);

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

        when(slaComplianceRepository.save(any(SlaCompliance.class)))
            .thenAnswer(invocation -> {
                SlaCompliance compliance = invocation.getArgument(0);
                compliance.setId(1L);
                return compliance;
            });

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
        LocalDateTime completed = LocalDateTime.now().plusHours(5);  // Took 5 hours, SLA was 4

        when(slaComplianceRepository.save(any(SlaCompliance.class)))
            .thenAnswer(invocation -> {
                SlaCompliance compliance = invocation.getArgument(0);
                compliance.setId(1L);
                return compliance;
            });

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
            .id(1L)
            .ticketNumber("TKT-20240529-0001")
            .alertStatus("SENT")
            .build();

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
            .id(1L)
            .ticketNumber("TKT-20240529-0001")
            .alertLevel("RED")
            .remainingSeconds(300L)
            .remainingTimeDisplay("5m 0s")
            .alertStatus("PENDING")
            .build();

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
            SlaAlert.builder().id(1L).ticketNumber("TKT-001").alertLevel("RED").alertStatus("SENT").build(),
            SlaAlert.builder().id(2L).ticketNumber("TKT-002").alertLevel("ORANGE").alertStatus("PENDING").build()
        );

        when(slaAlertRepository.list(any())).thenReturn(alerts);

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
            SlaAlert.builder().id(1L).ticketNumber("TKT-001").alertLevel("RED").alertStatus("SENT").build()
        );

        when(slaAlertRepository.list(any())).thenReturn(alerts);

        // Act
        List<SlaAlert> result = slaAlertService.getActiveAlerts("RED");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("RED", result.get(0).getAlertLevel());
    }
}