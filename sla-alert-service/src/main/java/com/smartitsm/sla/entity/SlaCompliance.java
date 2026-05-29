package com.smartitsm.sla.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * SLA compliance record entity.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sla_compliance")
public class SlaCompliance extends BaseEntity {

    private Long ticketId;
    private String ticketNumber;

    // SLA type
    private String slaType;             // FIRST_RESPONSE, RESOLUTION

    // Timing
    private LocalDateTime slaDueAt;
    private LocalDateTime completedAt;
    private Long targetSeconds;
    private Long actualSeconds;

    // Compliance result
    private Boolean met;
    private Long overageSeconds;
    private Double compliancePercentage;

    // SLA tier
    private String slaTier;
    private String ticketPriority;

    // Period for aggregation
    private String periodType;          // DAILY, WEEKLY, MONTHLY, QUARTERLY
    private String periodStart;
    private String periodEnd;
}