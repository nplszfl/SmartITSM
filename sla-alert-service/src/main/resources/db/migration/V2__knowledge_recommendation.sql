-- SmartITSM Knowledge Recommendation
-- V2__knowledge_recommendation.sql

CREATE TABLE IF NOT EXISTS t_knowledge_recommendation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT 'Ticket receiving the recommendation',
    knowledge_id BIGINT NOT NULL COMMENT 'Recommended knowledge article ID',

    relevance_score DOUBLE NOT NULL COMMENT 'Relevance 0..1',
    match_reason VARCHAR(255) COMMENT 'Why this article matched (category / keywords)',

    recommended_at DATETIME NOT NULL,
    clicked BOOLEAN DEFAULT FALSE,
    clicked_at DATETIME,
    helpful BOOLEAN DEFAULT FALSE,
    feedback_at DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INT DEFAULT 0,

    INDEX idx_rec_ticket (ticket_id),
    INDEX idx_rec_knowledge (knowledge_id),
    INDEX idx_rec_score (relevance_score),
    INDEX idx_rec_clicked (clicked),
    INDEX idx_rec_helpful (helpful)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Knowledge article recommendations per ticket';