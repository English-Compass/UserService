# 사용자 서비스 (User Service)

Spring Boot 기반의 사용자 관리 서비스로, Kakao 소셜 로그인을 지원합니다.

## 주요 기능

- 🟨 Kakao OAuth2 로그인
- 👤 사용자 정보 관리
- 🔑 JWT 토큰 기반 인증
- 💾 MySQL 데이터베이스
- 🚀 Redis 캐싱
- 🐳 Docker 컨테이너 지원

## 기술 스택

- **Backend**: Spring Boot 3.5.4, Java 17
- **Security**: Spring Security, OAuth2, JWT
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Build Tool**: Gradle
- **Container**: Docker, Docker Compose

## 빠른 시작

### 1. 환경 설정

```bash
# zshrc에 환경변수 설정
export KAKAO_CLIENT_ID="your-kakao-client-id"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
export JWT_SECRET="your-jwt-secret-key"
export JWT_EXPIRATION="86400000"

# 환경변수 적용
source ~/.zshrc
```

### 2. Docker로 실행

```bash
# 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 서비스 중지
docker-compose down
```

### 3. 로컬 개발 환경

```bash
# MySQL과 Redis 실행 (Docker)
docker-compose up -d db redis

# 애플리케이션 실행
./gradlew bootRun
```

## OAuth2 설정

### Kakao OAuth2

1. [Kakao Developers](https://developers.kakao.com/)에서 앱 생성
2. REST API 키 확인
3. 카카오 로그인 활성화
4. 리디렉션 URI: `http://localhost:8080/login/oauth2/code/kakao`
5. 동의항목 설정 (닉네임, 이메일)

## API 엔드포인트

- `GET /` - 메인 페이지
- `GET /login` - 로그인 페이지
- `GET /oauth2/authorization/kakao` - 카카오 로그인 시작
- `GET /login/oauth2/code/kakao` - OAuth2 콜백
- `GET /user/profile` - 사용자 프로필 (인증 필요)
- `POST /user/logout` - 로그아웃

## 데이터베이스 스키마

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    provider ENUM('KAKAO', 'LOCAL') NOT NULL,
    provider_id VARCHAR(255),
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `KAKAO_CLIENT_ID` | Kakao OAuth2 클라이언트 ID | - |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 클라이언트 시크릿 | - |
| `JWT_SECRET` | JWT 서명 키 | - |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | 86400000 |

## 개발 가이드

### 프로젝트 구조

```
src/main/java/com/example/demo/
├── DemoApplication.java          # 메인 애플리케이션
├── config/                       # 설정 클래스
├── controller/                   # REST 컨트롤러
├── entity/                       # JPA 엔티티
├── repository/                   # 데이터 접근 계층
├── service/                      # 비즈니스 로직
└── security/                     # 보안 관련 클래스
```

### 빌드 및 테스트

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

## 문제 해결

### 일반적인 문제

1. **데이터베이스 연결 실패**
   - MySQL 서비스가 실행 중인지 확인
   - 데이터베이스 접속 정보 확인

2. **카카오 로그인 실패**
   - 클라이언트 ID/시크릿 확인
   - 리디렉션 URI 설정 확인
   - 네트워크 연결 상태 확인

3. **JWT 토큰 오류**
   - JWT_SECRET 환경변수 설정 확인
   - 토큰 만료 시간 확인

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
