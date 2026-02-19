-- V3__add_keywords_column.sql
-- articles 테이블에 keywords 컬럼 추가 (TF-IDF 기반 핵심 키워드)

ALTER TABLE articles ADD COLUMN keywords VARCHAR(500) NULL AFTER content;