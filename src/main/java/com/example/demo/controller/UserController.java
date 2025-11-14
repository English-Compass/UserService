package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.dto.UserSettingsResponseDto;
import com.example.demo.service.UserService;
import com.example.demo.service.UserCategoryService;
import com.example.demo.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 사용자 관련 API 엔드포인트를 제공하는 REST 컨트롤러
 * 
 * 주요 기능:
 * - 사용자 프로필 조회
 * - 로그아웃 처리
 * - 인증 상태 확인
 * 
 * 모든 엔드포인트는 인증된 사용자만 접근 가능
 */
@RestController // REST API 컨트롤러임을 명시
@RequestMapping("/user") // 기본 경로를 /user로 설정
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 log 필드 자동 생성
public class UserController {
    
    /**
     * 사용자 관련 비즈니스 로직을 처리하는 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserService userService;
    
    /**
     * 사용자 카테고리 관련 비즈니스 로직을 처리하는 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserCategoryService userCategoryService;
    
    /**
     * 사용자 정보 캐싱을 위한 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserCacheService userCacheService;
    
    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 사용자 프로필 정보 (JSON 형태)
     *         - 사용자를 찾을 수 없는 경우: 404 Not Found
     *         - 성공 시: 200 OK와 함께 사용자 정보
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestHeader("X-User-Id") String userId) {
        try {
            // 사용자 정보를 데이터베이스에서 조회
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("name", user.getName());
            response.put("profileImage", user.getProfileImage());
            response.put("difficultyLevel", user.getDifficultyLevel());
            response.put("createdAt", user.getCreatedAt());
            
            log.info("User profile retrieved successfully: userId={}, name={}", userId, user.getName());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("User not found: userId={}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Error getting user profile: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 프로필 편집
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @param request 프로필 수정 요청 (name, profileImage)
     * @return 수정된 사용자 프로필 정보
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        try {
            // 사용자 정보를 데이터베이스에서 조회
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
            
            // 요청에서 수정할 정보 추출
            String name = (String) request.get("name");
            String profileImage = (String) request.get("profileImage");
            
            // 이름 업데이트
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name);
            }
            
            // 프로필 이미지 업데이트
            if (profileImage != null) {
                user.setProfileImage(profileImage);
            }
            
            // 사용자 정보 저장
            User updatedUser = userService.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", updatedUser.getUserId());
            response.put("name", updatedUser.getName());
            response.put("profileImage", updatedUser.getProfileImage());
            response.put("message", "프로필이 성공적으로 수정되었습니다");
            
            log.info("User profile updated: userId={}, name={}", userId, updatedUser.getName());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("User not found: userId={}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Error updating user profile: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    
    /**
     * 사용자 카테고리 조회
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 사용자 카테고리 정보
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getUserCategories(
            @RequestHeader("X-User-Id") String userId) {
        try {
            // userId를 통해 카테고리 정보 조회
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("categories", categories);
            
            log.info("User categories retrieved: userId={}, count={}", userId, categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user categories: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 설정 정보 조회
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 사용자 설정 정보 (프로필, 난이도, 카테고리)
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getUserSettings(
            @RequestHeader("X-User-Id") String userId) {
        try {
            // userId를 통해 사용자 설정 정보 조회
            UserSettingsResponseDto settingsResponse = userService.getUserSettings(userId);
            
            // userId를 통해 카테고리 정보 조회
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(userId);
            settingsResponse.addData("categories", categories);
            
            log.info("User settings retrieved: userId={}", userId);
            
            return ResponseEntity.ok(settingsResponse.toMap());
            
        } catch (Exception e) {
            log.error("Error getting user settings: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 난이도 설정 (별도 엔드포인트)
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @param request 난이도 레벨
     * @return 설정 결과
     */
    @PostMapping("/settings/difficulty")
    public ResponseEntity<Map<String, Object>> setUserDifficulty(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody Map<String, Object> request) {
        
        log.info("=== POST /user/settings/difficulty 요청 수신 ===");
        log.info("X-User-Id header: {}", userId);
        log.info("Request body: {}", request);
        log.info("Request method: POST");
        
        try {
            // X-User-Id 헤더 검증
            if (userId == null || userId.trim().isEmpty()) {
                log.error("X-User-Id header is missing or empty");
                return ResponseEntity.status(401).body(Map.of("error", "X-User-Id header is required"));
            }
            
            log.info("Received difficulty request: userId={}, request={}", userId, request);
            
            // difficultyLevel 추출 (Integer 또는 String으로 올 수 있음)
            Object difficultyLevelObj = request.get("difficultyLevel");
            Integer difficultyLevel = null;
            
            if (difficultyLevelObj instanceof Integer) {
                difficultyLevel = (Integer) difficultyLevelObj;
            } else if (difficultyLevelObj instanceof String) {
                try {
                    difficultyLevel = Integer.parseInt((String) difficultyLevelObj);
                } catch (NumberFormatException e) {
                    log.error("Invalid difficulty level format: {}", difficultyLevelObj);
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid difficulty level format"));
                }
            } else if (difficultyLevelObj instanceof Number) {
                difficultyLevel = ((Number) difficultyLevelObj).intValue();
            }
            
            if (difficultyLevel == null || difficultyLevel < 1 || difficultyLevel > 3) {
                log.error("Invalid difficulty level: userId={}, level={}", userId, difficultyLevel);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid difficulty level. Must be 1, 2, or 3"));
            }
            
            // userId를 통해 난이도 설정
            User updatedUser = userService.setUserDifficultyLevel(userId, difficultyLevel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "난이도 레벨이 성공적으로 설정되었습니다");
            response.put("userId", userId);
            response.put("difficultyLevel", updatedUser.getDifficultyLevel());
            
            log.info("User difficulty set successfully: userId={}, level={}", userId, updatedUser.getDifficultyLevel());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting user difficulty: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * 사용자 카테고리 설정 (전체 교체 - PUT 사용)
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @param request 카테고리 정보
     * @return 설정 결과
     */
    @PutMapping("/settings/categories")
    public ResponseEntity<Map<String, Object>> setUserCategories(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("Received category request: userId={}, request={}", userId, request);
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> categories = (Map<String, List<String>>) request.get("categories");
            
            if (categories == null || categories.isEmpty()) {
                log.warn("Categories are null or empty for userId={}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "Categories are required"));
            }
            
            log.info("Processing categories: userId={}, categories={}", userId, categories);
            
            // userId를 통해 카테고리 설정
            userCategoryService.saveUserCategories(userId, categories);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "카테고리가 성공적으로 설정되었습니다");
            response.put("userId", userId);
            response.put("categories", categories);
            
            log.info("User categories set: userId={}, categories={}", userId, categories);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid category data: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting user categories: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * 사용자 초기 설정 완료 여부 조회
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 사용자 설정 완료 여부
     */
    @GetMapping("/settings/status")
    public ResponseEntity<?> getUserSetupStatus(
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            // 사용자 정보 조회
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
            
            // 사용자 초기 설정 완료 여부 확인
            boolean hasCompletedSetup = false;
            
            // 1. 난이도 설정 확인
            if (user.getDifficultyLevel() != null) {
                // 2. 카테고리 설정 확인
                var categories = userCategoryService.getUserCategories(userId);
                if (categories != null && !categories.isEmpty()) {
                    hasCompletedSetup = true;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasCompletedSetup", hasCompletedSetup);
            response.put("userId", userId);
            response.put("name", user.getName());
            response.put("profileImage", user.getProfileImage());
            
            log.info("User setup status checked: userId={}, hasCompletedSetup={}", userId, hasCompletedSetup);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("User not found: userId={}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Error checking user setup status: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 현재 인증된 사용자 정보 조회 (프론트엔드용)
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/token/info")
    public ResponseEntity<?> getTokenInfo(
            @RequestHeader("X-User-Id") String userId) {
        
        try {
            // 사용자 정보 조회
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("name", user.getName());
            response.put("profileImage", user.getProfileImage());
            response.put("providerId", user.getProviderId());
            
            log.info("User info retrieved: userId={}, name={}", userId, user.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("User not found: userId={}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Error getting user info: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 캐시 상태 확인 (디버깅용)
     * 
     * @param userId API Gateway에서 헤더로 전달하는 사용자 ID
     * @return 캐시 상태 정보
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus(
            @RequestHeader("X-User-Id") String userId) {
        try {
            // 캐시에서 직접 조회
            Integer cachedDifficulty = userCacheService.getUserDifficulty(userId);
            Map<String, List<String>> cachedCategories = userCacheService.getUserCategories(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("cacheStatus", Map.of(
                "difficulty", Map.of(
                    "cached", cachedDifficulty != null,
                    "value", cachedDifficulty != null ? cachedDifficulty : "null"
                ),
                "categories", Map.of(
                    "cached", cachedCategories != null,
                    "count", cachedCategories != null ? cachedCategories.size() : 0
                )
            ));
            
            log.info("Cache status checked: userId={}, difficultyCached={}, categoriesCached={}", 
                    userId, cachedDifficulty != null, cachedCategories != null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking cache status: userId={}", userId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    
}
