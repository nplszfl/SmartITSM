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
import java.util.Map;

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

    /**
     * Get alerts by ticket.
     */
    @GetMapping("/alerts/ticket/{ticketId}")
    public List<SlaAlert> getAlertsByTicket(@PathVariable Long ticketId) {
        return slaAlertService.getAlertsByTicket(ticketId);
    }

    /**
     * Get breached alerts.
     */
    @GetMapping("/alerts/breached")
    public List<SlaAlert> getBreachedAlerts() {
        return slaAlertService.getBreachedAlerts();
    }

    /**
     * Get alerts near breach.
     */
    @GetMapping("/alerts/near-breach")
    public List<SlaAlert> getAlertsNearBreach() {
        return slaAlertService.getAlertsNearBreach();
    }

    /**
     * Get alert summary by level.
     */
    @GetMapping("/alerts/summary")
    public Map<String, Long> getAlertSummary() {
        return slaAlertService.getAlertSummary();
    }

    /**
     * Get top breaches.
     */
    @GetMapping("/alerts/top-breaches")
    public List<SlaAlert> getTopBreaches(@RequestParam(defaultValue = "10") int limit) {
        return slaAlertService.getTopBreaches(limit);
    }

    /**
     * Resolve an alert.
     */
    @PostMapping("/alerts/{alertId}/resolve")
    public SlaAlert resolveAlert(@PathVariable Long alertId) {
        return slaAlertService.resolveAlert(alertId);
    }

    /**
     * Bulk acknowledge alerts by tier.
     */
    @PostMapping("/alerts/bulk-acknowledge")
    public int bulkAcknowledgeByTier(
            @RequestParam String slaTier,
            @RequestParam String acknowledgedBy) {
        return slaAlertService.bulkAcknowledgeByTier(slaTier, acknowledgedBy);
    }

    /**
     * Clear resolved alerts.
     */
    @DeleteMapping("/alerts/clear")
    public int clearResolvedAlerts(@RequestParam(defaultValue = "30") int olderThanDays) {
        return slaAlertService.clearResolvedAlerts(olderThanDays);
    }

    /**
     * Estimate resolution time.
     */
    @GetMapping("/estimate-resolution")
    public Double estimateResolutionTime(
            @RequestParam Long ticketId,
            @RequestParam(required = false) String slaType) {
        return slaAlertService.estimateResolutionTime(ticketId, slaType);
    }
}