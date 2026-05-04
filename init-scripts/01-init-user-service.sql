-- =============================================
-- 사용자 서비스 데이터베이스 초기화 스크립트
-- MySQL 컨테이너가 처음 시작될 때 자동 실행됩니다.
-- =============================================

-- 문자셋 설정 (한글 지원을 위해 필수)
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- 데이터베이스 생성 (MYSQL_DATABASE 환경변수로도 생성되지만 명시적으로 생성)
CREATE DATABASE IF NOT EXISTS user_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'user_service_user'@'%' IDENTIFIED BY 'user_service_password';
GRANT ALL PRIVILEGES ON user_service_db.* TO 'user_service_user'@'%';
FLUSH PRIVILEGES;

-- 데이터베이스 선택
USE user_service_db;

-- =============================================
-- 테이블 생성
-- =============================================

-- 1. USERS 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '내부 ID (DB용)',
    user_id VARCHAR(36) NOT NULL UNIQUE COMMENT '외부 노출용 사용자 UUID',
    name VARCHAR(255) NOT NULL COMMENT '사용자 이름 (닉네임)',
    profile_image VARCHAR(500) NULL COMMENT '사용자 프로필 이미지 URL',
    provider_id VARCHAR(255) NULL COMMENT '제공자에서 제공하는 사용자 ID (카카오 등)',
    difficulty_level INT NULL COMMENT '사용자 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '계정 생성 시간',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '정보 마지막 수정 시간',
    
    -- 인덱스
    INDEX idx_user_id (user_id),
    INDEX idx_provider_id (provider_id),
    INDEX idx_created_at (created_at),
    INDEX idx_difficulty_level (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보 테이블';

-- 2. USER_CATEGORIES 테이블
CREATE TABLE IF NOT EXISTS user_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 고유 식별자',
    user_id BIGINT NOT NULL COMMENT '사용자 내부 ID (외래키, users.id 참조)',
    major_category VARCHAR(50) NOT NULL COMMENT '대분류 (STUDY, BUSINESS, TRAVEL, DAILY_LIFE)',
    minor_category VARCHAR(50) NOT NULL COMMENT '소분류 (CLASS_LISTENING, MEETING_CONFERENCE 등)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '카테고리 선택 시간',
    
    -- 외래키 제약조건 (users.id 참조)
    CONSTRAINT fk_user_categories_user_id 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    
    -- 유니크 제약조건 (한 사용자가 같은 카테고리를 중복 선택하지 못하도록)
    CONSTRAINT uk_user_major_minor_category 
        UNIQUE (user_id, major_category, minor_category),
    
    -- 인덱스
    INDEX idx_user_id (user_id),
    INDEX idx_major_category (major_category),
    INDEX idx_minor_category (minor_category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 선택 카테고리 테이블';

-- =============================================
-- 제약조건 추가
-- =============================================
-- 주의: MySQL 8.0.16 이상에서만 CHECK 제약조건 지원
-- 이미 존재하면 에러 발생하지만 무시됨

-- 대분류 유효성 체크
ALTER TABLE user_categories 
ADD CONSTRAINT chk_major_category 
CHECK (major_category IN ('STUDY', 'BUSINESS', 'TRAVEL', 'DAILY_LIFE'));

-- 소분류 유효성 체크
ALTER TABLE user_categories 
ADD CONSTRAINT chk_minor_category 
CHECK (minor_category IN (
    'CLASS_LISTENING', 'DEPARTMENT_CONVERSATION', 'ASSIGNMENT_EXAM',
    'MEETING_CONFERENCE', 'CUSTOMER_SERVICE', 'EMAIL_REPORT',
    'BACKPACKING', 'FAMILY_TRIP', 'FRIEND_TRIP',
    'SHOPPING_DINING', 'HOSPITAL_VISIT', 'PUBLIC_TRANSPORT'
));

-- 난이도 레벨 유효성 체크
ALTER TABLE users 
ADD CONSTRAINT chk_difficulty_level 
CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 3);

-- =============================================
-- 샘플 데이터 삽입 (중복 방지)
-- =============================================

-- 샘플 사용자 데이터 (중복 시 무시)
-- user_id는 UUID이므로 자동 생성되거나 명시적으로 지정 가능
INSERT IGNORE INTO users (user_id, name, profile_image, provider_id, difficulty_level) VALUES
('550e8400-e29b-41d4-a716-446655440000', '김영희', 'https://example.com/profile1.jpg', 'kakao_12345', 2),
('550e8400-e29b-41d4-a716-446655440001', '박민수', 'https://example.com/profile2.jpg', 'kakao_67890', 1),
('550e8400-e29b-41d4-a716-446655440002', '이지은', NULL, 'kakao_11111', 3);

-- 샘플 사용자 카테고리 데이터 (중복 시 무시)
-- user_id는 users 테이블의 id (내부 ID)를 참조
-- 위에서 삽입한 사용자들의 id를 참조 (AUTO_INCREMENT로 1, 2, 3이 생성됨)
INSERT IGNORE INTO user_categories (user_id, major_category, minor_category) VALUES
(1, 'STUDY', 'CLASS_LISTENING'),
(1, 'STUDY', 'DEPARTMENT_CONVERSATION'),
(1, 'BUSINESS', 'MEETING_CONFERENCE'),
(2, 'TRAVEL', 'BACKPACKING'),
(2, 'DAILY_LIFE', 'SHOPPING_DINING'),
(3, 'STUDY', 'ASSIGNMENT_EXAM'),
(3, 'BUSINESS', 'CUSTOMER_SERVICE'),
(3, 'TRAVEL', 'FAMILY_TRIP');

-- =============================================
-- 뷰 생성
-- =============================================

-- 사용자별 카테고리 정보를 조인한 뷰
CREATE OR REPLACE VIEW v_user_categories AS
SELECT 
    u.id as user_internal_id,
    u.user_id as user_uuid,
    u.name,
    u.profile_image,
    u.difficulty_level,
    uc.major_category,
    uc.minor_category,
    uc.created_at as category_selected_at
FROM users u
LEFT JOIN user_categories uc ON u.id = uc.user_id
ORDER BY u.id, uc.major_category, uc.minor_category;

-- =============================================
-- 초기화 완료
-- =============================================
