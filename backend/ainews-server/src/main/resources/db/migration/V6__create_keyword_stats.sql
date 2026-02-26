CREATE TABLE keyword_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    ai_count INT NOT NULL DEFAULT 0,
    rank_position INT NOT NULL,
    stat_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_keyword_date (keyword, stat_date),
    INDEX idx_date_rank (stat_date, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;