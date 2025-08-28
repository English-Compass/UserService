-- =====================================================
-- UserService Database Initialization Script
-- =====================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS user_service
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE user_service;

-- =====================================================
-- users 테이블 (사용자 정보)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 식별자',
    name VARCHAR(100) NOT NULL COMMENT '사용자 이름 (닉네임)',
    profile_image VARCHAR(500) COMMENT '사용자 프로필 이미지 URL',
    provider_id VARCHAR(100) COMMENT 'OAuth2 제공자에서 제공하는 사용자 ID',
    difficulty_level INT DEFAULT 2 COMMENT '사용자 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '사용자 계정 생성 시간',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '사용자 정보 마지막 수정 시간',
    
    -- 인덱스
    INDEX idx_provider_id (provider_id),
    INDEX idx_difficulty_level (difficulty_level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보 테이블';

-- =====================================================
-- user_categories 테이블 (사용자 선택 카테고리)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 고유 식별자',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (users 테이블 참조)',
    major_category VARCHAR(100) NOT NULL COMMENT '대분류 (예: 기술, 디자인, 마케팅)',
    minor_category VARCHAR(100) NOT NULL COMMENT '소분류 (예: 프로그래밍, UI/UX, 디지털마케팅)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '카테고리 선택 시간',
    
    -- 외래키 제약조건
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- 인덱스
    INDEX idx_user_id (user_id),
    INDEX idx_major_category (major_category),
    INDEX idx_minor_category (minor_category),
    INDEX idx_created_at (created_at),
    
    -- 복합 인덱스 (사용자별 카테고리 조회 최적화)
    INDEX idx_user_major (user_id, major_category),
    INDEX idx_user_minor (user_id, minor_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 선택 카테고리 테이블';

-- =====================================================
-- 샘플 데이터 삽입 (테스트용)
-- =====================================================

-- 샘플 사용자 데이터
INSERT INTO users (name, profile_image, provider_id, difficulty_level) VALUES
('테스트사용자1', 'https://example.com/profile1.jpg', 'kakao_12345', 2),
('테스트사용자2', 'https://example.com/profile2.jpg', 'kakao_67890', 1),
('테스트사용자3', 'https://example.com/profile3.jpg', 'kakao_11111', 3);

-- 샘플 카테고리 데이터
INSERT INTO user_categories (user_id, major_category, minor_category) VALUES
(1, '기술', '프로그래밍'),
(1, '기술', '데이터베이스'),
(1, '디자인', 'UI/UX'),
(2, '마케팅', '디지털마케팅'),
(2, '기술', '웹개발'),
(3, '디자인', '그래픽디자인'),
(3, '기술', '모바일개발');

-- =====================================================
-- 테이블 정보 확인
-- =====================================================
-- SHOW TABLES;
-- DESCRIBE users;
-- DESCRIBE user_categories;

-- =====================================================
-- 샘플 데이터 확인
-- =====================================================
-- SELECT * FROM users;
-- SELECT * FROM user_categories;
-- SELECT u.name, uc.major_category, uc.minor_category 
-- FROM users u 
-- JOIN user_categories uc ON u.user_id = uc.user_id;
