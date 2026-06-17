-- SmartITSM SLA Breach Risk Prediction
-- V1__sla_breach_risk.sql

CREATE TABLE IF NOT EXISTS t_sla_breach_risk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT 'Ticket being evaluated',
    sla_policy_id BIGINT COMMENT 'SLA policy reference',

    current_progress DOUBLE COMMENT 'Reported progress 0..100',
    expected_progress DOUBLE COMMENT 'Expected progress 0..100',

    risk_score DOUBLE NOT NULL COMMENT 'Predicted breach probability 0..1',
    predicted_breach_at DATETIME COMMENT 'Estimated breach timestamp',

    status VARCHAR(32) NOT NULL DEFAULT 'PREDICTED' COMMENT 'PREDICTED / HIGH_RISK / WATCH / SAFE / CRITICAL / BREACHED',
    ticket_category VARCHAR(64) COMMENT 'Ticket category at evaluation time',
    ticket_priority VARCHAR(32) COMMENT 'Ticket priority at evaluation time',
    sla_due_at DATETIME COMMENT 'Original SLA deadline',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INT DEFAULT 0,

    INDEX idx_risk_ticket (ticket_id),
    INDEX idx_risk_score (risk_score),
    INDEX idx_risk_status (status),
    INDEX idx_risk_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA breach risk predictions';