-- SmartITSM Database Schema Migration
-- V1__Initial_Schema.sql

-- ============================================
-- TICKET SERVICE TABLES
-- ============================================

CREATE TABLE t_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50), -- INCIDENT, SERVICE_REQUEST, CHANGE_REQUEST, PROBLEM
    sub_category VARCHAR(100),
    item VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW', -- NEW, OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- CRITICAL, HIGH, MEDIUM, LOW
    urgency VARCHAR(20) DEFAULT 'MEDIUM',
    impact VARCHAR(20) DEFAULT 'MEDIUM',
    assigned_to VARCHAR(100),
    assigned_group VARCHAR(100),
    assignment_reason VARCHAR(255),
    requester_id VARCHAR(100),
    requester_name VARCHAR(100),
    requester_email VARCHAR(255),
    requester_department VARCHAR(100),
    requester_priority VARCHAR(20) DEFAULT 'NORMAL', -- VIP, NORMAL
    sla_tier VARCHAR(10), -- P1, P2, P3, P4
    first_response_due DATETIME,
    resolution_due DATETIME,
    first_response_at DATETIME,
    resolved_at DATETIME,
    closed_at DATETIME,
    ai_score DOUBLE, -- AI-calculated priority score (0-100)
    ai_confidence VARCHAR(20), -- HIGH, MEDIUM, LOW
    ai_reasoning TEXT,
    ai_recommended_action VARCHAR(255),
    predicted_resolution_hours INT,
    suggested_assignee VARCHAR(100),
    asset_id BIGINT,
    asset_name VARCHAR(255),
    workflow_instance_id BIGINT,
    current_workflow_step VARCHAR(100),
    affected_users INT DEFAULT 0,
    business_value VARCHAR(50),
    downtime_impact VARCHAR(50),
    location VARCHAR(100),
    resolution_code VARCHAR(50), -- FIXED, WORKAROUND, DUPLICATE, CANNOT_REPRODUCE, etc.
    resolution_notes TEXT,
    closure_notes TEXT,
    source VARCHAR(30) DEFAULT 'PORTAL', -- PORTAL, EMAIL, PHONE, CHAT, API
    channel VARCHAR(30),
    external_ticket_id VARCHAR(100),
    satisfaction_rating INT,
    satisfaction_comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted INT DEFAULT 0,
    INDEX idx_ticket_number (ticket_number),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_category (category),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_requester_id (requester_id),
    INDEX idx_ai_score (ai_score),
    INDEX idx_resolution_due (resolution_due),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TICKET COMMENTS/HISTORY
-- ============================================

CREATE TABLE t_ticket_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    author_id VARCHAR(100),
    author_name VARCHAR(100),
    author_type VARCHAR(20), -- REQUESTER, AGENT, SYSTEM, AI
    is_internal BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_ticket_id (ticket_id),
    FOREIGN KEY (ticket_id) REFERENCES t_ticket(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_ticket_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    field_name VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(100),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket_id (ticket_id),
    FOREIGN KEY (ticket_id) REFERENCES t_ticket(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- ASSET SERVICE TABLES
-- ============================================

CREATE TABLE t_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_tag VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50), -- SERVER, NETWORK, STORAGE, ENDPOINT, SOFTWARE, LICENSE
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    purchase_date DATE,
    warranty_expiry DATE,
    warranty_status VARCHAR(20), -- ACTIVE, EXPIRED, NONE
    location VARCHAR(100),
    department VARCHAR(100),
    assigned_to VARCHAR(100),
    assigned_to_name VARCHAR(100),
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, MAINTENANCE, RETIRED
    health_score DOUBLE, -- AI-calculated health score (0-100)
    health_risk_level VARCHAR(20), -- CRITICAL, HIGH, MEDIUM, LOW
    health_prediction VARCHAR(255),
    last_health_check DATETIME,
    next_maintenance_date DATE,
    failure_probability DOUBLE,
    cost_centre VARCHAR(50),
    parent_asset_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted INT DEFAULT 0,
    INDEX idx_asset_tag (asset_tag),
    INDEX idx_asset_type (asset_type),
    INDEX idx_status (status),
    INDEX idx_assigned_to (assigned_to),
    FOREIGN KEY (parent_asset_id) REFERENCES t_asset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_asset_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE,
    metric_unit VARCHAR(20),
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_id (asset_id),
    INDEX idx_metric_name (metric_name),
    INDEX idx_recorded_at (recorded_at),
    FOREIGN KEY (asset_id) REFERENCES t_asset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_asset_incident_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    incident_count INT DEFAULT 0,
    incident_types VARCHAR(255),
    last_incident_date DATETIME,
    avg_resolution_hours DOUBLE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES t_asset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- WORKFLOW SERVICE TABLES
-- ============================================

CREATE TABLE t_workflow_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    workflow_type VARCHAR(50), -- TICKET_ROUTING, APPROVAL, NOTIFICATION, ESCALATION
    version INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    trigger_condition VARCHAR(255), -- JSON condition for when workflow applies
    steps JSON, -- Array of workflow steps
    total_steps INT,
    avg_completion_hours DOUBLE,
    sla_compliance_rate DOUBLE,
    current_cost DECIMAL(10,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted INT DEFAULT 0,
    INDEX idx_workflow_type (workflow_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_workflow_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_definition_id BIGINT NOT NULL,
    entity_type VARCHAR(50), -- TICKET, ASSET, CHANGE
    entity_id BIGINT NOT NULL,
    current_step_index INT DEFAULT 0,
    current_step_name VARCHAR(100),
    status VARCHAR(30) DEFAULT 'RUNNING', -- RUNNING, COMPLETED, CANCELLED, FAILED
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    initiated_by VARCHAR(100),
    context_data JSON, -- Additional context for workflow
    FOREIGN KEY (workflow_definition_id) REFERENCES t_workflow_definition(id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_workflow_step_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    step_index INT,
    step_name VARCHAR(100),
    action_taken VARCHAR(100),
    performed_by VARCHAR(100),
    performed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(30), -- SUCCESS, FAILED, SKIPPED
    notes TEXT,
    FOREIGN KEY (workflow_instance_id) REFERENCES t_workflow_instance(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- ANALYTICS SERVICE TABLES
-- ============================================

CREATE TABLE t_analytics_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50), -- COUNTER, GAUGE, HISTOGRAM
    entity_type VARCHAR(50), -- TICKET, ASSET, USER
    entity_id BIGINT,
    value DOUBLE NOT NULL,
    unit VARCHAR(20),
    dimension JSON, -- Additional dimensions/tags
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_name (metric_name),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_ai_insight (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    insight_type VARCHAR(50), -- TREND, ANOMALY, RECOMMENDATION, PREDICTION
    title VARCHAR(255) NOT NULL,
    description TEXT,
    confidence DOUBLE,
    severity VARCHAR(20), -- CRITICAL, WARNING, INFO
    recommendations JSON,
    affected_entity_type VARCHAR(50),
    affected_entity_ids JSON,
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at DATETIME,
    INDEX idx_insight_type (insight_type),
    INDEX idx_severity (severity),
    INDEX idx_generated_at (generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE t_dashboard_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    config JSON, -- Widget layout and settings
    is_default BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- SLA CONFIGURATION
-- ============================================

CREATE TABLE t_sla_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    priority VARCHAR(20),
    urgency VARCHAR(20),
    impact VARCHAR(20),
    response_time_hours INT,
    resolution_time_hours INT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- SERVICE CATALOG
-- ============================================

CREATE TABLE t_service_catalog_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    sub_category VARCHAR(100),
    workflow_definition_id BIGINT,
    cost DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workflow_definition_id) REFERENCES t_workflow_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;