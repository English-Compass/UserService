-- =============================================
-- UserService Database Schema
-- MySQL DDL for User and Category Management
-- Spring Boot 호환 버전 (DELIMITER, PREPARE/EXECUTE 제거)
-- =============================================

-- 데이터베이스 생성 (필요한 경우)
ㅇ-- CREATE DATABASE IF NOT EXISTS user_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE user_service_db;

-- =============================================
-- 1. USERS 테이블
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 고유 식별자',
    name VARCHAR(255) NOT NULL COMMENT '사용자 이름 (닉네임)',
    profile_image VARCHAR(500) NULL COMMENT '사용자 프로필 이미지 URL',
    provider_id VARCHAR(255) NULL COMMENT '제공자에서 제공하는 사용자 ID (카카오 등)',
    difficulty_level INT NULL COMMENT '사용자 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '계정 생성 시간',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '정보 마지막 수정 시간',
    
    -- 인덱스
    INDEX idx_provider_id (provider_id),
    INDEX idx_created_at (created_at),
    INDEX idx_difficulty_level (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보 테이블';

-- =============================================
-- 2. USER_CATEGORIES 테이블
-- =============================================
CREATE TABLE IF NOT EXISTS user_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '카테고리 고유 식별자',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (외래키)',
    major_category VARCHAR(50) NOT NULL COMMENT '대분류 (STUDY, BUSINESS, TRAVEL, DAILY_LIFE)',
    minor_category VARCHAR(50) NOT NULL COMMENT '소분류 (CLASS_LISTENING, MEETING_CONFERENCE 등)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '카테고리 선택 시간',
    
    -- 외래키 제약조건
    CONSTRAINT fk_user_categories_user_id 
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
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
-- 3. 카테고리 유효성 검사를 위한 체크 제약조건
-- =============================================
-- 주의: MySQL 8.0.16 이상에서만 CHECK 제약조건 지원
-- 이미 존재하면 에러 발생하지만 continue-on-error=true로 무시됨

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
-- 4. 샘플 데이터 삽입 (중복 방지)
-- =============================================

-- 샘플 사용자 데이터 (중복 시 무시)
INSERT IGNORE INTO users (name, profile_image, provider_id, difficulty_level) VALUES
('김영희', 'https://example.com/profile1.jpg', 'kakao_12345', 2),
('박민수', 'https://example.com/profile2.jpg', 'kakao_67890', 1),
('이지은', NULL, 'kakao_11111', 3);

-- 샘플 사용자 카테고리 데이터 (중복 시 무시)
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
-- 5. 뷰 생성 (선택사항)
-- =============================================

-- 사용자별 카테고리 정보를 조인한 뷰
CREATE OR REPLACE VIEW v_user_categories AS
SELECT 
    u.user_id,
    u.name,
    u.profile_image,
    u.difficulty_level,
    uc.major_category,
    uc.minor_category,
    uc.created_at as category_selected_at
FROM users u
LEFT JOIN user_categories uc ON u.user_id = uc.user_id
ORDER BY u.user_id, uc.major_category, uc.minor_category;

-- =============================================
-- 6. 저장 프로시저 (선택사항 - 수동 실행 필요)
-- =============================================
-- 주의: Spring Boot의 SQL 스크립트 실행기는 DELIMITER를 지원하지 않으므로
-- 저장 프로시저는 MySQL 클라이언트에서 수동으로 실행해야 합니다.
-- 
-- 사용자 카테고리 추가 프로시저:
-- DELIMITER //
-- CREATE PROCEDURE AddUserCategory(
--     IN p_user_id BIGINT,
--     IN p_major_category VARCHAR(50),
--     IN p_minor_category VARCHAR(50)
-- )
-- BEGIN
--     DECLARE EXIT HANDLER FOR SQLEXCEPTION
--     BEGIN
--         ROLLBACK;
--         RESIGNAL;
--     END;
--     
--     START TRANSACTION;
--     
--     IF EXISTS (
--         SELECT 1 FROM user_categories 
--         WHERE user_id = p_user_id 
--         AND major_category = p_major_category 
--         AND minor_category = p_minor_category
--     ) THEN
--         SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '이미 선택된 카테고리입니다.';
--     END IF;
--     
--     INSERT INTO user_categories (user_id, major_category, minor_category)
--     VALUES (p_user_id, p_major_category, p_minor_category);
--     
--     COMMIT;
-- END //
-- DELIMITER ;
--
-- 사용자 카테고리 삭제 프로시저:
-- DELIMITER //
-- CREATE PROCEDURE RemoveUserCategory(
--     IN p_user_id BIGINT,
--     IN p_major_category VARCHAR(50),
--     IN p_minor_category VARCHAR(50)
-- )
-- BEGIN
--     DECLARE EXIT HANDLER FOR SQLEXCEPTION
--     BEGIN
--         ROLLBACK;
--         RESIGNAL;
--     END;
--     
--     START TRANSACTION;
--     
--     DELETE FROM user_categories 
--     WHERE user_id = p_user_id 
--     AND major_category = p_major_category 
--     AND minor_category = p_minor_category;
--     
--     COMMIT;
-- END //
-- DELIMITER ;

-- =============================================
-- 스키마 생성 완료
-- =============================================
