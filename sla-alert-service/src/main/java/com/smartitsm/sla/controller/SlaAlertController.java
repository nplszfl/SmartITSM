package com.smartitsm.sla.controller;

import com.smartitsm.sla.dto.SlaCountdownResponse;
import com.smartitsm.sla.dto.SlaMonitorRequest;
import com.smartitsm.sla.dto.SlaStatsResponse;
import com.smartitsm.sla.entity.SlaAlert;
import com.smartitsm.sla.entity.SlaCompliance;
import com.smartitsm.sla.service.SlaAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API controller for SLA monitoring and alerts.
 */
@RestController
@RequestMapping("/api/v1/sla")
@RequiredArgsConstructor
public class SlaAlertController {

    private final SlaAlertService slaAlertService;

    /**
     * Monitor SLA for a ticket.
     */
    @PostMapping("/monitor")
    public SlaAlert monitorSla(@RequestBody SlaMonitorRequest request) {
        return slaAlertService.monitorSla(request);
    }

    /**
     * Get SLA countdown for a specific ticket.
     */
    @GetMapping("/countdown/{ticketId}")
    public SlaCountdownResponse getCountdown(
            @PathVariable Long ticketId,
            @RequestParam(required = false) String slaType) {
        return slaAlertService.getSlaCountdown(ticketId, slaType);
    }

    /**
     * Get SLA compliance statistics.
     */
    @GetMapping("/stats")
    public SlaStatsResponse getStats(
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate) {
        return slaAlertService.getSlaStats(periodType, startDate, endDate);
    }

    /**
     * Get all active alerts.
     */
    @GetMapping("/alerts")
    public List<SlaAlert> getActiveAlerts(@RequestParam(required = false) String alertLevel) {
        return slaAlertService.getActiveAlerts(alertLevel);
    }

    /**
     * Acknowledge an alert.
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public SlaAlert acknowledgeAlert(@PathVariable Long alertId) {
        return slaAlertService.acknowledgeAlert(alertId);
    }

    /**
     * Record SLA compliance.
     */
    @PostMapping("/compliance")
    public SlaCompliance recordCompliance(
            @RequestParam Long ticketId,
            @RequestParam String ticketNumber,
            @RequestParam String slaType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slaDueAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime completedAt,
            @RequestParam(required = false) String slaTier,
            @RequestParam(required = false) String ticketPriority) {
        return slaAlertService.recordCompliance(ticketId, ticketNumber, slaType, slaDueAt, completedAt, slaTier, ticketPriority);
    }

    /**
     * Send alert notification.
     */
    @PostMapping("/alerts/{alertId}/send")
    public void sendAlert(@PathVariable Long alertId, @RequestParam String channel) {
        SlaAlert alert = slaAlertService.getActiveAlerts(null).stream()
            .filter(a -> a.getId().equals(alertId))
            .findFirst()
            .orElse(null);
        if (alert != null) {
            slaAlertService.sendAlert(alert, channel);
        }
    }
}