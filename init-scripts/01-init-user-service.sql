-- 사용자 서비스 데이터베이스 초기화
-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS user_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'user_service_user'@'%' IDENTIFIED BY 'user_service_password';
GRANT ALL PRIVILEGES ON user_service_db.* TO 'user_service_user'@'%';
FLUSH PRIVILEGES;

-- 데이터베이스 선택
USE user_service_db;

-- 사용자 테이블 생성 (JPA가 자동으로 생성하지만, 초기 구조를 미리 정의)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    provider ENUM('GOOGLE', 'KAKAO', 'LOCAL') NOT NULL,
    provider_id VARCHAR(255),
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_provider_provider_id (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 샘플 관리자 사용자 생성 (필요시)
-- INSERT INTO users (email, name, provider, provider_id, role) VALUES ('admin@example.com', 'Admin User', 'LOCAL', 'admin', 'ADMIN');
