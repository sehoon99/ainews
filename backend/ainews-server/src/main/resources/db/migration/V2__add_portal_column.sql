-- V2__add_portal_column.sql
-- articles 테이블에 portal 컬럼 추가 (뉴스 출처 포털 구분)

ALTER TABLE articles
ADD COLUMN portal ENUM('naver', 'daum', 'others') NOT NULL DEFAULT 'others'
AFTER author_id;

-- portal 컬럼 인덱스 추가 (포털별 조회 성능 향상)
CREATE INDEX idx_portal ON articles(portal);
