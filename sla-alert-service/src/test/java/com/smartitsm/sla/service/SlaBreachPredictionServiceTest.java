package com.smartitsm.sla.service;

import com.smartitsm.sla.dto.SlaBreachPredictRequest;
import com.smartitsm.sla.dto.SlaBreachRiskDto;
import com.smartitsm.sla.entity.SlaBreachRisk;
import com.smartitsm.sla.entity.SlaCompliance;
import com.smartitsm.sla.repository.SlaAlertRepository;
import com.smartitsm.sla.repository.SlaBreachRiskRepository;
import com.smartitsm.sla.repository.SlaComplianceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlaBreachPredictionService.
 *
 * Covers:
 *  - 4-factor weighted scoring (mathematical correctness)
 *  - boundary conditions (zero gap, missing inputs)
 *  - high-risk threshold filtering
 *  - recording actual breach
 *  - persistence
 *  - empty / null safety
 */
@ExtendWith(MockitoExtension.class)
class SlaBreachPredictionServiceTest {

    @Mock
    private SlaBreachRiskRepository breachRiskRepository;

    @Mock
    private SlaAlertRepository slaAlertRepository;

    @Mock
    private SlaComplianceRepository slaComplianceRepository;

    @InjectMocks
    private SlaBreachPredictionService service;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2024, 5, 29, 12, 0);
    }

    // -------- Algorithm: factor components --------

    @Test
    @DisplayName("progressGap: expected=80, current=30 -> 0.5")
    void testComputeProgressGap_PartialGap() {
        assertEquals(0.5, service.computeProgressGap(30.0, 80.0), 1e-9);
    }

    @Test
    @DisplayName("progressGap: current >= expected -> 0")
    void testComputeProgressGap_NoGap() {
        assertEquals(0.0, service.computeProgressGap(95.0, 80.0), 1e-9);
        assertEquals(0.0, service.computeProgressGap(80.0, 80.0), 1e-9);
    }

    @Test
    @DisplayName("progressGap: null inputs -> 0")
    void testComputeProgressGap_NullInputs() {
        assertEquals(0.0, service.computeProgressGap(null, 50.0));
        assertEquals(0.0, service.computeProgressGap(50.0, null));
        assertEquals(0.0, service.computeProgressGap(null, null));
    }

    @Test
    @DisplayName("timeElapsedRatio: half elapsed -> 0.5")
    void testComputeTimeElapsedRatio_HalfElapsed() {
        LocalDateTime started = now.minusHours(4);
        LocalDateTime due = now.plusHours(4);
        assertEquals(0.5, service.computeTimeElapsedRatio(started, due, now), 1e-9);
    }

    @Test
    @DisplayName("timeElapsedRatio: overdue -> clamped to 1.0")
    void testComputeTimeElapsedRatio_Overdue() {
        LocalDateTime started = now.minusHours(10);
        LocalDateTime due = now.minusHours(1);
        assertEquals(1.0, service.computeTimeElapsedRatio(started, due, now), 1e-9);
    }

    @Test
    @DisplayName("priorityFactor: HIGH=0.8, MEDIUM=0.5, LOW=0.2, default=0.3")
    void testPriorityFactor() {
        assertEquals(0.8, service.priorityFactor("HIGH"), 1e-9);
        assertEquals(0.8, service.priorityFactor("CRITICAL"), 1e-9);
        assertEquals(0.5, service.priorityFactor("MEDIUM"), 1e-9);
        assertEquals(0.2, service.priorityFactor("LOW"), 1e-9);
        assertEquals(0.3, service.priorityFactor("UNKNOWN"), 1e-9);
        assertEquals(0.3, service.priorityFactor(null), 1e-9);
    }

    // -------- Algorithm: full pipeline --------

    @Test
    @DisplayName("predictRisk: high-risk scenario yields score >= 0.7 and HIGH/CRITICAL level")
    void testPredictRisk_HighRiskScenario() {
        SlaBreachPredictRequest req = SlaBreachPredictRequest.builder()
                .ticketId(100L)
                .ticketCategory("INCIDENT")
                .ticketPriority("HIGH")
                .currentProgress(0.0)   // worst case: 100% gap
                .expectedProgress(100.0)
                .slaStartedAt(now.minusHours(4))
                .slaDueAt(now.minusHours(1)) // past due: timeElapsed=1.0
                .now(now)
                .build();

        // 30/100 historical breach rate (will bias score up via w3=0.20)
        when(slaComplianceRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(
                        breachedCompliance(true),
                        breachedCompliance(false),
                        breachedCompliance(false),
                        breachedCompliance(true),
                        breachedCompliance(true),
                        breachedCompliance(false),
                        breachedCompliance(false),
                        breachedCompliance(false),
                        breachedCompliance(false),
                        breachedCompliance(false)
                ));
        lenient().when(breachRiskRepository.save(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRiskDto dto = service.predictRisk(req);

        assertNotNull(dto);
        assertEquals(100L, dto.getTicketId());
        assertNotNull(dto.getRiskScore());
        assertTrue(dto.getRiskScore() >= 0.7,
                "Expected high-risk score but got " + dto.getRiskScore());
        assertTrue(dto.getRiskLevel().equals("HIGH") || dto.getRiskLevel().equals("CRITICAL"));
        assertNotNull(dto.getPredictedBreachAt());
        verify(breachRiskRepository, times(1)).save(any(SlaBreachRisk.class));
    }

    @Test
    @DisplayName("predictRisk: low-risk scenario yields score below threshold")
    void testPredictRisk_LowRiskScenario() {
        SlaBreachPredictRequest req = SlaBreachPredictRequest.builder()
                .ticketId(200L)
                .ticketCategory("SERVICE_REQUEST")
                .ticketPriority("LOW")
                .currentProgress(95.0)
                .expectedProgress(80.0)
                .slaStartedAt(now.minusHours(1))
                .slaDueAt(now.plusHours(8))
                .now(now)
                .build();

        // 0/5 historical breach rate
        when(slaComplianceRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(
                        breachedCompliance(true),
                        breachedCompliance(true),
                        breachedCompliance(true),
                        breachedCompliance(true),
                        breachedCompliance(true)
                ));
        lenient().when(breachRiskRepository.save(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRiskDto dto = service.predictRisk(req);

        assertNotNull(dto);
        assertTrue(dto.getRiskScore() < 0.7,
                "Expected low-risk score but got " + dto.getRiskScore());
        assertEquals("LOW", dto.getRiskLevel());
        assertNotNull(dto.getProgressGapFactor());
        assertNotNull(dto.getTimeElapsedRatioFactor());
        assertNotNull(dto.getHistoricalBreachRateFactor());
        assertNotNull(dto.getPriorityFactor());
    }

    @Test
    @DisplayName("predictRisk: score is clamped to [0,1]")
    void testPredictRisk_ClampedRange() {
        SlaBreachPredictRequest req = SlaBreachPredictRequest.builder()
                .ticketId(300L)
                .ticketPriority("CRITICAL")
                .currentProgress(0.0)
                .expectedProgress(200.0) // over-gap => clamped
                .slaStartedAt(now.minusDays(1))
                .slaDueAt(now.minusMinutes(1))
                .now(now)
                .build();

        // no stubbing - service short-circuits when category is null
        lenient().when(breachRiskRepository.save(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRiskDto dto = service.predictRisk(req);

        assertNotNull(dto.getRiskScore());
        assertTrue(dto.getRiskScore() >= 0.0);
        assertTrue(dto.getRiskScore() <= 1.0,
                "Risk score must be <= 1.0 but was " + dto.getRiskScore());
    }

    @Test
    @DisplayName("predictRisk: missing inputs default safely and persist")
    void testPredictRisk_MissingInputsSafe() {
        SlaBreachPredictRequest req = SlaBreachPredictRequest.builder()
                .ticketId(400L)
                // no progress, no priority, no dates
                .build();
        // compliance list is NOT stubbed: service short-circuits when category is null.
        lenient().when(breachRiskRepository.save(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRiskDto dto = service.predictRisk(req);

        assertNotNull(dto);
        // All factors collapse to defaults: progressGap=0, timeElapsed=0,
        // historical=0.1 (low prior), priority=0.3.
        // rawScore = 0.35*0 + 0.30*0 + 0.20*0.1 + 0.15*0.3 = 0.065
        assertEquals(0.065, dto.getRiskScore(), 1e-9);
        assertEquals("LOW", dto.getRiskLevel());
        ArgumentCaptor<SlaBreachRisk> captor = ArgumentCaptor.forClass(SlaBreachRisk.class);
        verify(breachRiskRepository).save(captor.capture());
        assertEquals(400L, captor.getValue().getTicketId());
    }

    // -------- Filtering --------

    @Test
    @DisplayName("getHighRiskTickets: filters by threshold and sorts desc")
    void testGetHighRiskTickets_FiltersAndSorts() {
        SlaBreachRisk low = SlaBreachRisk.builder().ticketId(1L).riskScore(0.5).build();
        low.setId(10L);
        SlaBreachRisk mid = SlaBreachRisk.builder().ticketId(2L).riskScore(0.8).build();
        mid.setId(11L);
        SlaBreachRisk high = SlaBreachRisk.builder().ticketId(3L).riskScore(0.95).build();
        high.setId(12L);

        when(breachRiskRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(high, mid, low));

        List<SlaBreachRiskDto> result = service.getHighRiskTickets(0.7);

        assertEquals(2, result.size(), "Should filter out riskScore < 0.7");
        // desc order
        assertTrue(result.get(0).getRiskScore() >= result.get(1).getRiskScore());
    }

    @Test
    @DisplayName("getHighRiskTickets: empty repo yields empty list")
    void testGetHighRiskTickets_Empty() {
        when(breachRiskRepository.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        List<SlaBreachRiskDto> result = service.getHighRiskTickets(0.7);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------- Breach recording --------

    @Test
    @DisplayName("recordActualBreach: updates existing risk to BREACHED + 1.0")
    void testRecordActualBreach_UpdatesExisting() {
        SlaBreachRisk existing = SlaBreachRisk.builder()
                .ticketId(500L)
                .riskScore(0.6)
                .status("HIGH_RISK")
                .build();
        existing.setId(99L);
        when(breachRiskRepository.getOne(any())).thenReturn(existing);
        when(breachRiskRepository.updateById(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRisk result = service.recordActualBreach(500L);

        assertEquals("BREACHED", result.getStatus());
        assertEquals(1.0, result.getRiskScore(), 1e-9);
        verify(breachRiskRepository).updateById(any(SlaBreachRisk.class));
    }

    @Test
    @DisplayName("recordActualBreach: creates new record when none exists")
    void testRecordActualBreach_CreatesNew() {
        when(breachRiskRepository.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);
        when(breachRiskRepository.save(any(SlaBreachRisk.class))).thenReturn(true);

        SlaBreachRisk result = service.recordActualBreach(600L);

        assertEquals(600L, result.getTicketId());
        assertEquals("BREACHED", result.getStatus());
        assertEquals(1.0, result.getRiskScore(), 1e-9);
        verify(breachRiskRepository).save(any(SlaBreachRisk.class));
    }

    // -------- Risk level classification --------

    @Test
    @DisplayName("toRiskLevel: bucket boundaries are correct")
    void testRiskLevelBuckets() {
        assertEquals("CRITICAL", service.toRiskLevel(0.95));
        assertEquals("HIGH", service.toRiskLevel(0.75));
        assertEquals("MEDIUM", service.toRiskLevel(0.5));
        assertEquals("LOW", service.toRiskLevel(0.1));
    }

    @Test
    @DisplayName("determineStatus: maps score to PREDICTED/HIGH_RISK/WATCH/SAFE/CRITICAL")
    void testDetermineStatus() {
        assertEquals("CRITICAL", service.determineStatus(0.95));
        assertEquals("HIGH_RISK", service.determineStatus(0.8));
        assertEquals("WATCH", service.determineStatus(0.5));
        assertEquals("SAFE", service.determineStatus(0.1));
    }

    // -------- Helpers --------

    private SlaCompliance breachedCompliance(boolean met) {
        return SlaCompliance.builder()
                .ticketId(1L)
                .slaType("RESOLUTION")
                .met(met)
                .build();
    }
}