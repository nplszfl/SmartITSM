-- V1__Create_asset_table.sql
CREATE TABLE IF NOT EXISTS t_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Asset identifier (AST-YYYY-NNNNN)',
    name VARCHAR(200) NOT NULL COMMENT 'Asset name',
    description TEXT COMMENT 'Detailed description',
    
    -- Classification
    asset_type VARCHAR(50) NOT NULL COMMENT 'SERVER, NETWORK, STORAGE, ENDPOINT, SOFTWARE, CLOUD, DATABASE',
    sub_type VARCHAR(50) COMMENT 'Specific type within category',
    manufacturer VARCHAR(100) COMMENT 'OEM/manufacturer',
    model VARCHAR(100) COMMENT 'Model number',
    serial_number VARCHAR(100) COMMENT 'Hardware serial number',
    
    -- Status & Health
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, MAINTENANCE, RETIRED, DISPOSED',
    health_status VARCHAR(20) DEFAULT 'UNKNOWN' COMMENT 'HEALTHY, WARNING, CRITICAL, UNKNOWN',
    health_score DOUBLE COMMENT 'AI-calculated health score (0-100)',
    ai_recommendations TEXT COMMENT 'AI maintenance recommendations',
    
    -- Location & Ownership
    location VARCHAR(200) COMMENT 'Physical or logical location',
    department VARCHAR(100) COMMENT 'Owning department',
    assigned_to VARCHAR(100) COMMENT 'Assigned user',
    assigned_group VARCHAR(100) COMMENT 'Responsible team',
    
    -- Technical Details
    ip_address VARCHAR(50) COMMENT 'IP address',
    mac_address VARCHAR(50) COMMENT 'MAC address',
    hostname VARCHAR(100) COMMENT 'Network hostname',
    operating_system VARCHAR(100) COMMENT 'OS version',
    cpu_cores INT COMMENT 'CPU core count',
    memory_gb INT COMMENT 'Memory in GB',
    storage_gb INT COMMENT 'Storage capacity in GB',
    
    -- Metrics
    cpu_usage DOUBLE COMMENT 'Current CPU usage %',
    memory_usage DOUBLE COMMENT 'Current memory usage %',
    disk_usage DOUBLE COMMENT 'Current disk usage %',
    network_latency DOUBLE COMMENT 'Network latency in ms',
    error_rate DOUBLE COMMENT 'Error rate %',
    last_health_check DATETIME COMMENT 'Last health check timestamp',
    last_monitoring DATETIME COMMENT 'Last monitoring data update',
    
    -- Lifecycle
    purchase_date DATE COMMENT 'Purchase date',
    warranty_expiry DATE COMMENT 'Warranty expiration',
    warranty_status VARCHAR(20) COMMENT 'ACTIVE, EXPIRED, EXTENDED',
    installation_date DATE COMMENT 'Installation date',
    retirement_date DATE COMMENT 'Planned retirement date',
    expected_life_years INT COMMENT 'Expected useful life in years',
    
    -- Financial
    purchase_cost DECIMAL(15,2) COMMENT 'Purchase cost',
    maintenance_cost DECIMAL(15,2) COMMENT 'Annual maintenance cost',
    replacement_cost DECIMAL(15,2) COMMENT 'Estimated replacement cost',
    
    -- Relationships
    parent_asset_id BIGINT COMMENT 'Parent asset in hierarchy',
    related_ticket_ids TEXT COMMENT 'Comma-separated ticket IDs',
    configuration_items TEXT COMMENT 'Related CIs as JSON',
    
    -- Risk Assessment
    failure_probability DOUBLE COMMENT 'AI-calculated failure probability',
    risk_level VARCHAR(20) COMMENT 'CRITICAL, HIGH, MEDIUM, LOW',
    business_impact VARCHAR(50) COMMENT 'Impact if asset fails',
    
    -- Maintenance
    last_maintenance_date DATE COMMENT 'Last maintenance performed',
    next_maintenance_date DATE COMMENT 'Next scheduled maintenance',
    maintenance_interval_days INT COMMENT 'Days between maintenance',
    maintenance_compliance DOUBLE COMMENT 'Maintenance compliance %',
    
    -- Metadata
    vendor VARCHAR(100) COMMENT 'Vendor/supplier',
    vendor_contact VARCHAR(200) COMMENT 'Vendor contact info',
    support_level VARCHAR(20) COMMENT 'BASIC, STANDARD, PREMIUM, PREMIUM_PLUS',
    tags VARCHAR(500) COMMENT 'Comma-separated tags',
    
    -- Common fields
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted TINYINT DEFAULT 0,
    
    INDEX idx_asset_number (asset_number),
    INDEX idx_asset_type (asset_type),
    INDEX idx_status (status),
    INDEX idx_health_status (health_status),
    INDEX idx_assigned_group (assigned_group),
    INDEX idx_risk_level (risk_level),
    INDEX idx_parent_asset (parent_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IT Asset inventory';
