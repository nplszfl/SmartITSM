package com.smartitsm.sla.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartitsm.sla.dto.SlaCountdownResponse;
import com.smartitsm.sla.dto.SlaMonitorRequest;
import com.smartitsm.sla.dto.SlaStatsResponse;
import com.smartitsm.sla.entity.SlaAlert;
import com.smartitsm.sla.entity.SlaCompliance;
import com.smartitsm.sla.repository.SlaAlertRepository;
import com.smartitsm.sla.repository.SlaComplianceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SLA Alert Service - monitors SLA compliance, sends alerts, and tracks statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaAlertService {

    private final SlaAlertRepository slaAlertRepository;
    private final SlaComplianceRepository slaComplianceRepository;

    // Alert thresholds (in minutes)
    private static final long GREEN_THRESHOLD = 60;      // > 60 min remaining
    private static final long YELLOW_THRESHOLD = 30;     // 30-60 min remaining
    private static final long ORANGE_THRESHOLD = 15;    // 15-30 min remaining
    private static final long RED_THRESHOLD = 5;        // 5-15 min remaining
    private static final long BREACH_THRESHOLD = 0;     // <= 0 min = breach

    /**
     * Monitor SLA for a ticket and create/update alerts.
     */
    @Transactional
    public SlaAlert monitorSla(SlaMonitorRequest request) {
        log.info("Monitoring SLA for ticket: {}", request.getTicketNumber());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDueAt = request.getResolutionDue() != null 
            ? request.getResolutionDue() 
            : request.getFirstResponseDue();

        if (slaDueAt == null) {
            log.warn("No SLA deadline for ticket: {}", request.getTicketNumber());
            return null;
        }

        long remainingSeconds = Duration.between(now, slaDueAt).getSeconds();
        String alertLevel = calculateAlertLevel(remainingSeconds);
        String remainingTimeDisplay = formatRemainingTime(remainingSeconds);

        // Check if alert already exists
        SlaAlert existingAlert = findExistingAlert(request.getTicketId(), request.getSlaType());
        
        if (existingAlert != null) {
            // Update existing alert
            existingAlert.setRemainingSeconds(remainingSeconds);
            existingAlert.setRemainingTimeDisplay(remainingTimeDisplay);
            existingAlert.setAlertLevel(alertLevel);
            
            // Check for breach
            if (remainingSeconds <= 0 && !existingAlert.getBreached()) {
                existingAlert.setBreached(true);
                existingAlert.setBreachedAt(LocalDateTime.now());
                existingAlert.setBreachDurationSeconds(-remainingSeconds);
                existingAlert.setAlertStatus("BREACHED");
            }
            
            slaAlertRepository.updateById(existingAlert);
            return existingAlert;
        }

        // Create new alert
        SlaAlert alert = SlaAlert.builder()
            .ticketId(request.getTicketId())
            .ticketNumber(request.getTicketNumber())
            .slaType(request.getSlaType())
            .slaTier(request.getSlaTier())
            .slaDueAt(slaDueAt)
            .remainingSeconds(remainingSeconds)
            .remainingTimeDisplay(remainingTimeDisplay)
            .alertLevel(alertLevel)
            .alertStatus(determineAlertStatus(remainingSeconds))
            .notifyAssignedTo(request.getAssignedTo())
            .notifyAssignedGroup(request.getAssignedGroup())
            .ticketTitle(request.getTicketTitle())
            .ticketStatus(request.getTicketStatus())
            .ticketPriority(request.getTicketPriority())
            .assignedTo(request.getAssignedTo())
            .assignedGroup(request.getAssignedGroup())
            .reminderCount(0)
            .build();

        slaAlertRepository.save(alert);
        log.info("Created SLA alert for ticket {} with level {}", request.getTicketNumber(), alertLevel);
        
        return alert;
    }

    /**
     * Get SLA countdown for a specific ticket.
     */
    public SlaCountdownResponse getSlaCountdown(Long ticketId, String slaType) {
        SlaAlert alert = slaAlertRepository.getOne(
            new QueryWrapper<SlaAlert>()
                .eq("ticket_id", ticketId)
                .eq(slaType != null, "sla_type", slaType)
                .orderByDesc("created_at")
                .last("LIMIT 1")
        );

        if (alert == null) {
            return SlaCountdownResponse.builder()
                .ticketId(ticketId)
                .status("NO_SLA_DATA")
                .build();
        }

        // Calculate progress percentage
        long totalSeconds = Duration.between(alert.getCreatedAt(), alert.getSlaDueAt()).getSeconds();
        long elapsedSeconds = totalSeconds - alert.getRemainingSeconds();
        double progressPercentage = totalSeconds > 0 ? (elapsedSeconds * 100.0 / totalSeconds) : 0;

        return SlaCountdownResponse.builder()
            .ticketId(alert.getTicketId())
            .ticketNumber(alert.getTicketNumber())
            .slaType(alert.getSlaType())
            .slaTier(alert.getSlaTier())
            .remainingSeconds(alert.getRemainingSeconds())
            .remainingTimeDisplay(alert.getRemainingTimeDisplay())
            .progressPercentage(progressPercentage)
            .alertLevel(alert.getAlertLevel())
            .status(alert.getAlertStatus())
            .slaDueAt(alert.getSlaDueAt())
            .ticketTitle(alert.getTicketTitle())
            .ticketStatus(alert.getTicketStatus())
            .assignedTo(alert.getAssignedTo())
            .assignedGroup(alert.getAssignedGroup())
            .build();
    }

    /**
     * Get SLA compliance statistics.
     */
    public SlaStatsResponse getSlaStats(String periodType, String startDate, String endDate) {
        QueryWrapper<SlaCompliance> query = new QueryWrapper<>();
        
        if (periodType != null) {
            query.eq("period_type", periodType);
        }
        if (startDate != null) {
            query.ge("period_start", startDate);
        }
        if (endDate != null) {
            query.le("period_end", endDate);
        }

        List<SlaCompliance> compliances = slaComplianceRepository.list(query);

        long totalTickets = compliances.size();
        long metSla = compliances.stream().filter(c -> c.getMet() != null && c.getMet()).count();
        long breachedSla = totalTickets - metSla;
        double overallComplianceRate = totalTickets > 0 ? (metSla * 100.0 / totalTickets) : 0;

        // By SLA type
        double firstResponseCompliance = calculateComplianceByType(compliances, "FIRST_RESPONSE");
        double resolutionCompliance = calculateComplianceByType(compliances, "RESOLUTION");

        // By tier
        Map<String, SlaStatsResponse.SlaTierStats> tierStats = new HashMap<>();
        for (String tier : Arrays.asList("P1", "P2", "P3", "P4")) {
            List<SlaCompliance> tierCompliance = compliances.stream()
                .filter(c -> tier.equals(c.getSlaTier()))
                .collect(Collectors.toList());
            
            long tierTotal = tierCompliance.size();
            long tierMet = tierCompliance.stream().filter(c -> c.getMet() != null && c.getMet()).count();
            double tierComplianceRate = tierTotal > 0 ? (tierMet * 100.0 / tierTotal) : 0;

            // Calculate averages
            double avgResponse = tierCompliance.stream()
                .filter(c -> "FIRST_RESPONSE".equals(c.getSlaType()))
                .mapToLong(c -> c.getActualSeconds() != null ? c.getActualSeconds() : 0)
                .average().orElse(0);
            
            double avgResolution = tierCompliance.stream()
                .filter(c -> "RESOLUTION".equals(c.getSlaType()))
                .mapToLong(c -> c.getActualSeconds() != null ? c.getActualSeconds() : 0)
                .average().orElse(0);

            tierStats.put(tier, SlaStatsResponse.SlaTierStats.builder()
                .slaTier(tier)
                .total(tierTotal)
                .met(tierMet)
                .breached(tierTotal - tierMet)
                .complianceRate(tierComplianceRate)
                .avgResponseTimeMinutes(avgResponse / 60)
                .avgResolutionTimeMinutes(avgResolution / 60)
                .build());
        }

        return SlaStatsResponse.builder()
            .overallComplianceRate(overallComplianceRate)
            .totalTickets(totalTickets)
            .metSla(metSla)
            .breachedSla(breachedSla)
            .firstResponseCompliance(firstResponseCompliance)
            .resolutionCompliance(resolutionCompliance)
            .tierStats(tierStats)
            .periodType(periodType != null ? periodType : "ALL")
            .periodStart(startDate)
            .periodEnd(endDate)
            .build();
    }

    /**
     * Record SLA compliance when a ticket is resolved or closed.
     */
    @Transactional
    public SlaCompliance recordCompliance(Long ticketId, String ticketNumber, String slaType,
                                          LocalDateTime slaDueAt, LocalDateTime completedAt,
                                          String slaTier, String ticketPriority) {
        // Target = total SLA window from completedAt reference; but more clearly,
        // target = remaining time at the start (we approximate via Duration between now and slaDueAt as "remaining budget")
        // Actual = remaining time when completed (Duration between completedAt and slaDueAt)
        long targetSeconds = Duration.between(LocalDateTime.now(), slaDueAt).getSeconds();
        long actualSeconds = Duration.between(completedAt, slaDueAt).getSeconds();

        // Met if completedAt is on or before slaDueAt (i.e. actualSeconds >= 0)
        boolean met = actualSeconds >= 0;
        long overageSeconds = met ? 0 : (-actualSeconds);
        double compliancePercentage = targetSeconds > 0 ?
            Math.max(0, Math.min(100, (actualSeconds * 100.0 / targetSeconds))) : 0;

        SlaCompliance compliance = SlaCompliance.builder()
            .ticketId(ticketId)
            .ticketNumber(ticketNumber)
            .slaType(slaType)
            .slaDueAt(slaDueAt)
            .completedAt(completedAt)
            .targetSeconds(targetSeconds)
            .actualSeconds(actualSeconds)
            .met(met)
            .overageSeconds(overageSeconds)
            .compliancePercentage(compliancePercentage)
            .slaTier(slaTier)
            .ticketPriority(ticketPriority)
            .periodType(determinePeriodType(completedAt))
            .periodStart(determinePeriodStart(completedAt))
            .periodEnd(determinePeriodEnd(completedAt))
            .build();

        slaComplianceRepository.save(compliance);
        log.info("Recorded SLA compliance for ticket {}: {} - {}", ticketNumber, slaType, met ? "MET" : "BREACHED");
        
        return compliance;
    }

    /**
     * Get all active alerts.
     */
    public List<SlaAlert> getActiveAlerts(String alertLevel) {
        QueryWrapper<SlaAlert> query = new QueryWrapper<>();
        query.in("alert_status", Arrays.asList("PENDING", "SENT", "ACKNOWLEDGED"));
        
        if (alertLevel != null) {
            query.eq("alert_level", alertLevel);
        }
        
        return slaAlertRepository.list(query);
    }

    /**
     * Acknowledge an alert.
     */
    @Transactional
    public SlaAlert acknowledgeAlert(Long alertId) {
        SlaAlert alert = slaAlertRepository.getById(alertId);
        if (alert != null) {
            alert.setAlertStatus("ACKNOWLEDGED");
            slaAlertRepository.updateById(alert);
            log.info("Alert {} acknowledged for ticket {}", alertId, alert.getTicketNumber());
        }
        return alert;
    }

    /**
     * Send alert notification (simulated).
     */
    @Transactional
    public void sendAlert(SlaAlert alert, String channel) {
        log.info("Sending SLA alert via {} for ticket {} - Level: {}, Remaining: {}", 
            channel, alert.getTicketNumber(), alert.getAlertLevel(), alert.getRemainingTimeDisplay());

        alert.setAlertStatus("SENT");
        alert.setAlertSentAt(LocalDateTime.now());
        alert.setNotificationChannel(channel);
        alert.setReminderCount(alert.getReminderCount() != null ? alert.getReminderCount() + 1 : 1);
        
        slaAlertRepository.updateById(alert);
    }

    /**
     * Get SLA alerts for a specific ticket.
     */
    public List<SlaAlert> getAlertsByTicket(Long ticketId) {
        return slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .eq("ticket_id", ticketId)
                .orderByDesc("created_at"));
    }

    /**
     * Get SLA alerts by tier (P1, P2, P3, P4).
     */
    public List<SlaAlert> getAlertsByTier(String slaTier) {
        QueryWrapper<SlaAlert> query = new QueryWrapper<>();
        query.in("alert_status", Arrays.asList("PENDING", "SENT", "ACKNOWLEDGED"));
        if (slaTier != null) {
            query.eq("sla_tier", slaTier);
        }
        return slaAlertRepository.list(query);
    }

    /**
     * Get all breached SLA alerts.
     */
    public List<SlaAlert> getBreachedAlerts() {
        return slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .eq("breached", true)
                .orderByAsc("breached_at"));
    }

    /**
     * Get alerts approaching breach (RED or ORANGE level).
     */
    public List<SlaAlert> getAlertsNearBreach() {
        return slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .in("alert_level", Arrays.asList("RED", "ORANGE", "BREACHED"))
                .in("alert_status", Arrays.asList("PENDING", "SENT", "ACKNOWLEDGED"))
                .orderByAsc("remaining_seconds"));
    }

    /**
     * Clear resolved alerts older than specified days.
     */
    @Transactional
    public int clearResolvedAlerts(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        List<SlaAlert> resolvedAlerts = slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .eq("alert_status", "RESOLVED")
                .lt("updated_at", cutoff));
        
        int count = 0;
        for (SlaAlert alert : resolvedAlerts) {
            slaAlertRepository.removeById(alert.getId());
            count++;
        }
        log.info("Cleared {} resolved SLA alerts older than {} days", count, olderThanDays);
        return count;
    }

    /**
     * Resolve an alert manually.
     */
    @Transactional
    public SlaAlert resolveAlert(Long alertId) {
        SlaAlert alert = slaAlertRepository.getById(alertId);
        if (alert != null) {
            alert.setAlertStatus("RESOLVED");
            slaAlertRepository.updateById(alert);
            log.info("Alert {} resolved for ticket {}", alertId, alert.getTicketNumber());
        }
        return alert;
    }

    /**
     * Bulk acknowledge alerts by tier.
     */
    @Transactional
    public int bulkAcknowledgeByTier(String slaTier, String acknowledgedBy) {
        QueryWrapper<SlaAlert> query = new QueryWrapper<>();
        query.in("alert_status", Arrays.asList("PENDING", "SENT"))
            .eq("sla_tier", slaTier);
        
        List<SlaAlert> alerts = slaAlertRepository.list(query);
        for (SlaAlert alert : alerts) {
            alert.setAlertStatus("ACKNOWLEDGED");
            alert.setAlertMessage(alert.getAlertMessage() + " | Acknowledged by: " + acknowledgedBy);
            slaAlertRepository.updateById(alert);
        }
        log.info("Bulk acknowledged {} {} alerts by {}", alerts.size(), slaTier, acknowledgedBy);
        return alerts.size();
    }

    /**
     * Get alert summary by level.
     */
    public Map<String, Long> getAlertSummary() {
        Map<String, Long> summary = new HashMap<>();
        for (String level : Arrays.asList("GREEN", "YELLOW", "ORANGE", "RED", "BREACHED")) {
            summary.put(level, slaAlertRepository.count(
                new QueryWrapper<SlaAlert>()
                    .eq("alert_level", level)
                    .in("alert_status", Arrays.asList("PENDING", "SENT", "ACKNOWLEDGED"))));
        }
        return summary;
    }

    /**
     * Get top breaches (most overdue tickets).
     */
    public List<SlaAlert> getTopBreaches(int limit) {
        return slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .eq("breached", true)
                .orderByAsc("breach_duration_seconds")
                .last("LIMIT " + limit));
    }

    /**
     * Calculate estimated resolution time based on current pace.
     */
    public Double estimateResolutionTime(Long ticketId, String slaType) {
        SlaAlert alert = findExistingAlert(ticketId, slaType);
        if (alert == null || alert.getRemainingSeconds() <= 0) {
            return null;
        }
        // Simple estimate: based on remaining time and progress
        long totalSeconds = Duration.between(alert.getCreatedAt(), alert.getSlaDueAt()).getSeconds();
        double progress = 1.0 - ((double) alert.getRemainingSeconds() / totalSeconds);
        if (progress <= 0) return null;
        
        double avgSecondsPerPercent = (totalSeconds * progress) / (progress * 100);
        double remainingPercent = (double) alert.getRemainingSeconds() / totalSeconds * 100;
        return avgSecondsPerPercent * remainingPercent / 60; // in minutes
    }

    /**
     * Scheduled task to check and send alerts every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndSendAlerts() {
        log.debug("Running SLA alert check...");
        
        List<SlaAlert> pendingAlerts = slaAlertRepository.list(
            new QueryWrapper<SlaAlert>()
                .in("alert_status", Arrays.asList("PENDING", "SENT"))
                .orderByAsc("remaining_seconds")
        );

        for (SlaAlert alert : pendingAlerts) {
            if (alert.getRemainingSeconds() <= RED_THRESHOLD * 60) {
                // Send urgent alert
                sendAlert(alert, "EMAIL");
                sendAlert(alert, "SYSTEM");
            } else if (alert.getRemainingSeconds() <= ORANGE_THRESHOLD * 60) {
                // Send warning alert
                sendAlert(alert, "EMAIL");
            } else if (alert.getRemainingSeconds() <= YELLOW_THRESHOLD * 60) {
                // Update status
                alert.setAlertStatus("PENDING");
                slaAlertRepository.updateById(alert);
            }
        }
    }

    // Helper methods

    private String calculateAlertLevel(long remainingSeconds) {
        long remainingMinutes = remainingSeconds / 60;
        
        if (remainingSeconds <= BREACH_THRESHOLD * 60) return "BREACHED";
        if (remainingMinutes <= RED_THRESHOLD) return "RED";
        if (remainingMinutes <= ORANGE_THRESHOLD) return "ORANGE";
        if (remainingMinutes <= YELLOW_THRESHOLD) return "YELLOW";
        return "GREEN";
    }

    private String formatRemainingTime(long seconds) {
        if (seconds <= 0) {
            return "BREACHED " + formatDuration(-seconds);
        }
        return formatDuration(seconds);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%dd %dh %dm", days, hours, minutes);
        }
        return String.format("%dh %dm %ds", hours, minutes, secs);
    }

    private String determineAlertStatus(long remainingSeconds) {
        if (remainingSeconds <= 0) return "BREACHED";
        if (remainingSeconds <= RED_THRESHOLD * 60) return "URGENT";
        if (remainingSeconds <= YELLOW_THRESHOLD * 60) return "WARNING";
        return "NORMAL";
    }

    private SlaAlert findExistingAlert(Long ticketId, String slaType) {
        return slaAlertRepository.getOne(
            new QueryWrapper<SlaAlert>()
                .eq("ticket_id", ticketId)
                .eq(slaType != null, "sla_type", slaType)
                .orderByDesc("created_at")
                .last("LIMIT 1")
        );
    }

    private double calculateComplianceByType(List<SlaCompliance> compliances, String slaType) {
        List<SlaCompliance> filtered = compliances.stream()
            .filter(c -> slaType.equals(c.getSlaType()))
            .collect(Collectors.toList());
        
        long total = filtered.size();
        long met = filtered.stream().filter(c -> c.getMet() != null && c.getMet()).count();
        
        return total > 0 ? (met * 100.0 / total) : 0;
    }

    private String determinePeriodType(LocalDateTime dateTime) {
        return "DAILY";
    }

    private String determinePeriodStart(LocalDateTime dateTime) {
        return dateTime.toLocalDate().toString();
    }

    private String determinePeriodEnd(LocalDateTime dateTime) {
        return dateTime.toLocalDate().toString();
    }
}