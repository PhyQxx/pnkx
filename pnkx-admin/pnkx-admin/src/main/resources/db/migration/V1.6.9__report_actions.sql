CREATE TABLE px_report_insight (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,owner_id VARCHAR(64) NOT NULL,period VARCHAR(16) NOT NULL,
 period_start DATE NOT NULL,period_end DATE NOT NULL,comparison_start DATE NOT NULL,comparison_end DATE NOT NULL,
 actual_amount DECIMAL(18,2) NOT NULL,comparison_amount DECIMAL(18,2) NOT NULL,
 fact_text VARCHAR(1000) NOT NULL,status VARCHAR(24) NOT NULL DEFAULT 'proposed',
 plan_id VARCHAR(64) NULL,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_insight_owner(owner_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
