# UserService

사용자 인증 및 프로필 관리 서비스입니다. Kakao OAuth2 로그인을 처리하고 JWT 토큰을 발급합니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 17 |
| 빌드 도구 | Gradle |
| 프레임워크 | Spring Boot 3.5.4 |
| 포트 | 8081 |
| 인증 | Spring Security, Kakao OAuth2, JWT (JJWT 0.12.3) |
| 데이터베이스 | MySQL 8.0 |
| 캐시 | Redis |
| 메시징 | Kafka |

## 주요 기능

- **Kakao OAuth2 로그인**: 카카오 계정을 통한 소셜 로그인
- **JWT 발급**: 로그인 성공 시 API Gateway와 공유되는 JWT_SECRET으로 토큰 서명 발급
- **사용자 프로필 관리**: 학습 카테고리, 난이도 설정 저장
- **Redis 캐싱**: 사용자 세션 캐싱
- **Kafka 이벤트 발행**: 사용자 프로필 변경 이벤트 발행

## 인증 흐름

```
1. 클라이언트 → GET /oauth2/authorization/kakao
2. 카카오 로그인 페이지로 리다이렉트
3. 카카오 인증 완료 → GET /api/auth/oauth2/callback/kakao
4. UserService에서 카카오 사용자 정보 조회 및 DB 저장/업데이트
5. JWT 토큰 발급 후 클라이언트에 반환
6. 이후 모든 요청: Authorization: Bearer <token>
```

## API 엔드포인트

| Method | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| GET | `/oauth2/authorization/kakao` | 카카오 로그인 시작 | 불필요 |
| GET | `/api/auth/oauth2/callback/kakao` | OAuth2 콜백 처리 | 불필요 |
| GET | `/user/profile` | 사용자 프로필 조회 | 필요 |
| POST | `/user/logout` | 로그아웃 | 필요 |

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `KAKAO_API_KEY_USER_SERVICE` | Kakao OAuth2 클라이언트 ID | 필수 |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 클라이언트 시크릿 | 필수 |
| `OAUTH2_REDIRECT_URI` | OAuth2 콜백 URI | 선택 (기본값: `http://localhost:8080/api/auth/oauth2/callback/kakao`) |
| `JWT_SECRET` | JWT 서명 키 — api-gateway와 동일해야 함 | 필수 |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | 선택 (기본값: `86400000`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | 선택 (기본값: `localhost:9092`) |

## Kakao 개발자 설정

1. [Kakao Developers](https://developers.kakao.com/)에서 애플리케이션 생성
2. REST API 키 확인 후 `KAKAO_API_KEY_USER_SERVICE`에 설정
3. 카카오 로그인 활성화
4. 리디렉션 URI 등록: `http://localhost:8080/api/auth/oauth2/callback/kakao`
5. 동의항목: 닉네임, 프로필 사진 (이메일은 선택)

## 실행 방법

### 로컬 실행

```bash
# 인프라 먼저 실행 (api-gateway 디렉토리에서)
cd ../api-gateway && docker-compose up -d

cd UserService\(En\)

# 환경 변수 설정
export KAKAO_API_KEY_USER_SERVICE="your-kakao-client-id"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
export JWT_SECRET="your-jwt-secret"

./gradlew bootRun
```

### Docker

```bash
cd UserService\(En\)
docker-compose up --build -d
docker-compose logs -f app
```

## 데이터베이스 스키마

```sql
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) UNIQUE,
    name        VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    provider    ENUM('KAKAO') NOT NULL,
    provider_id VARCHAR(255),
    role        ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_category (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    major_category VARCHAR(100),
    minor_category VARCHAR(100),
    difficulty   VARCHAR(10)
);
```

## JWT 토큰 구조

발급되는 JWT에는 다음 클레임이 포함됩니다.

```json
{
  "sub": "userId",
  "role": "USER",
  "exp": 1234567890,
  "iat": 1234567890
}
```

api-gateway가 이 토큰을 직접 검증하므로, 두 서비스가 동일한 `JWT_SECRET`을 사용해야 합니다.

## Kafka 이벤트

| 토픽 | 방향 | 설명 |
|------|------|------|
| `user-profile-events` | 발행 | 사용자 프로필 생성/변경 시 발행, ProblemService에서 구독 |

## 프로젝트 구조

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── config/                      # Security, Redis 설정
├── controller/
│   ├── UserController.java
│   └── OAuth2CallbackController.java
├── entity/
│   ├── User.java
│   └── UserCategory.java
├── repository/
├── service/
│   ├── UserService.java
│   ├── KakaoOAuth2UserService.java
│   ├── UserCategoryService.java
│   ├── UserCacheService.java
│   └── KafkaProducerService.java
├── security/
│   ├── CustomUserDetails.java
│   └── JwtAuthenticationFilter.java
└── utils/
    └── JwtUtil.java
```

## 트러블슈팅

| 증상 | 원인 | 해결 방법 |
|------|------|-----------|
| 카카오 로그인 실패 | 잘못된 클라이언트 ID/시크릿 | 환경 변수 확인 |
| 카카오 로그인 실패 | 리디렉션 URI 불일치 | Kakao 개발자 콘솔에서 URI 등록 확인 |
| JWT 검증 실패 | api-gateway와 JWT_SECRET 불일치 | 두 서비스의 JWT_SECRET 동일 여부 확인 |
| DB 연결 실패 | MySQL 미실행 | `docker-compose up -d` (api-gateway 디렉토리) |
