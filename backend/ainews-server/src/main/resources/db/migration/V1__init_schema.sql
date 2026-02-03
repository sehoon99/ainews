-- V1__init_schema.sql
-- AI News 서비스 초기 스키마

-- 1. 언론사 정보 테이블
-- 기사가 어느 매체에서 발행되었는지 관리합니다.
CREATE TABLE providers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. 기자 정보 테이블
-- 특정 기자의 AI 이미지 활용 빈도 및 활동량을 추적합니다.
CREATE TABLE authors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    provider_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    article_count INT DEFAULT 0,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE,
    INDEX idx_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. 기사 메타데이터 및 본문 테이블
-- 크롤링된 기사의 전체 내용을 저장하며, Elasticsearch 색인의 원본이 됩니다.
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id INT NOT NULL,
    author_id INT,
    title VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    source_url VARCHAR(500) UNIQUE NOT NULL,
    published_at DATETIME,
    crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE SET NULL,
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. AI 이미지 분석 결과 테이블
-- 유료 API를 통한 분석 확률을 저장하며, image_hash로 중복 분석을 방지합니다.
CREATE TABLE image_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    image_hash VARCHAR(64) NOT NULL,
    api_name VARCHAR(50),
    ai_probability FLOAT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    INDEX idx_image_hash (image_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
