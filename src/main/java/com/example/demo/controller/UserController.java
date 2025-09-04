package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.UserCategoryService;
import com.example.demo.dto.CategoryRequestDto;
import com.example.demo.dto.CategoryResponseDto;
import com.example.demo.dto.UserSettingsResponseDto;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
     * JWT 토큰 생성을 위한 유틸리티
     * 생성자 주입으로 의존성 주입
     */
    private final JwtUtil jwtUtil;
    
    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @return 사용자 프로필 정보 (JSON 형태)
     *         - 인증되지 않은 경우: 401 Unauthorized
     *         - 사용자를 찾을 수 없는 경우: 500 Internal Server Error
     *         - 성공 시: 200 OK와 함께 사용자 정보
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 인증된 사용자가 없는 경우 처리
            if (userDetails == null) {
                log.warn("Unauthenticated user tried to access profile"); // 로그 기록
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            // 사용자 정보를 데이터베이스에서 조회
            User user = userService.findByProviderId(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("name", user.getName());
            response.put("profileImage", user.getProfileImage());
            response.put("difficultyLevel", user.getDifficultyLevel());
            response.put("createdAt", user.getCreatedAt());
            
            log.info("User profile retrieved successfully: {}", user.getName()); // 로그 기록
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user profile", e); // 에러 로그 기록
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    
    /**
     * 현재 사용자의 인증 상태를 확인
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @return 인증 상태 정보 (JSON 형태)
     *         - 인증된 경우: authenticated=true와 함께 사용자 정보
     *         - 인증되지 않은 경우: authenticated=false
     */
    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuthentication(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        
        if (userDetails != null) {
            // 인증된 사용자인 경우
            response.put("authenticated", true);
            response.put("email", userDetails.getUsername());
            response.put("authorities", userDetails.getAuthorities());
            log.debug("Authentication check: user {} is authenticated", userDetails.getUsername()); // 디버그 로그
        } else {
            // 인증되지 않은 사용자인 경우
            response.put("authenticated", false);
            log.debug("Authentication check: user is not authenticated"); // 디버그 로그
        }
        
        return ResponseEntity.ok(response);
    }
    
    
    
    /**
     * 사용자 카테고리 조회
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @return 사용자 카테고리 정보
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getUserCategories(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            // JWT 인증된 사용자의 경우 CustomUserDetails에서 직접 userId 가져오기
            Long userId;
            User user;
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                userId = customUserDetails.getUserId();
                user = customUserDetails.getUser();
            } else {
                // OAuth2 인증된 사용자의 경우 providerId로 조회
                user = userService.findByProviderId(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                userId = user.getUserId();
            }
            
            // userId를 통해 카테고리 정보 조회
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("categories", categories);
            
            log.info("User categories retrieved: userId={}, count={}", userId, categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user categories", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 설정 정보 조회
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @return 사용자 설정 정보 (프로필, 난이도, 카테고리)
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getUserSettings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            // JWT 인증된 사용자의 경우 CustomUserDetails에서 직접 userId 가져오기
            Long userId;
            User user;
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                userId = customUserDetails.getUserId();
                user = customUserDetails.getUser();
            } else {
                // OAuth2 인증된 사용자의 경우 providerId로 조회
                user = userService.findByProviderId(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                userId = user.getUserId();
            }
            
            // userId를 통해 사용자 설정 정보 조회
            UserSettingsResponseDto settingsResponse = userService.getUserSettings(userId);
            
            // userId를 통해 카테고리 정보 조회
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(userId);
            settingsResponse.addData("categories", categories);
            
            log.info("User settings retrieved: userId={}, name={}, difficultyLevel={}", 
                    userId, user.getName(), user.getDifficultyLevel());
            
            return ResponseEntity.ok(settingsResponse.toMap());
            
        } catch (Exception e) {
            log.error("Error getting user settings", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 난이도 설정 (별도 엔드포인트)
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @param request 난이도 레벨
     * @return 설정 결과
     */
    @PostMapping("/settings/difficulty")
    public ResponseEntity<Map<String, Object>> setUserDifficulty(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Integer> request) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            // JWT 인증된 사용자의 경우 CustomUserDetails에서 직접 userId 가져오기
            Long userId;
            User user;
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                userId = customUserDetails.getUserId();
                user = customUserDetails.getUser();
            } else {
                // OAuth2 인증된 사용자의 경우 providerId로 조회
                user = userService.findByProviderId(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                userId = user.getUserId();
            }
            
            Integer difficultyLevel = request.get("difficultyLevel");
            if (difficultyLevel == null || difficultyLevel < 1 || difficultyLevel > 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid difficulty level. Must be 1, 2, or 3"));
            }
            
            // userId를 통해 난이도 설정
            User updatedUser = userService.setUserDifficultyLevel(userId, difficultyLevel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "난이도 레벨이 성공적으로 설정되었습니다");
            response.put("userId", userId);
            response.put("difficultyLevel", updatedUser.getDifficultyLevel());
            
            log.info("User difficulty set: userId={}, level={}", userId, updatedUser.getDifficultyLevel());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting user difficulty", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 카테고리 설정 (별도 엔드포인트)
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @param request 카테고리 정보
     * @return 설정 결과
     */
    @PostMapping("/settings/categories")
    public ResponseEntity<Map<String, Object>> setUserCategories(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            // JWT 인증된 사용자의 경우 CustomUserDetails에서 직접 userId 가져오기
            Long userId;
            User user;
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                userId = customUserDetails.getUserId();
                user = customUserDetails.getUser();
            } else {
                // OAuth2 인증된 사용자의 경우 providerId로 조회
                user = userService.findByProviderId(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                userId = user.getUserId();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> categories = (Map<String, List<String>>) request.get("categories");
            
            if (categories == null || categories.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Categories are required"));
            }
            
            // userId를 통해 카테고리 설정
            userCategoryService.saveUserCategories(userId, categories);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "카테고리가 성공적으로 설정되었습니다");
            response.put("userId", userId);
            response.put("categories", categories);
            
            log.info("User categories set: userId={}, categories={}", userId, categories);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting user categories", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 초기 설정 완료 여부 조회
     * 
     * @param token JWT 토큰
     * @return 사용자 설정 완료 여부
     */
    @GetMapping("/settings/status")
    public ResponseEntity<?> getUserSetupStatus(
            @RequestHeader("Authorization") String token) {
        
        try {
            // JWT 토큰 검증
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token format"));
            }
            
            String jwt = token.substring(7);
            
            // JWT 토큰에서 사용자 정보 추출
            String providerId = jwtUtil.extractProviderId(jwt);
            String name = jwtUtil.extractName(jwt);
            String profileImage = jwtUtil.extractProfileImage(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            
            if (providerId == null || userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }
            
            // providerId로 사용자 조회 (검증용)
            User user = userService.findByProviderId(providerId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
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
            response.put("name", name != null ? name : user.getName());
            response.put("profileImage", profileImage != null ? profileImage : user.getProfileImage());
            
            log.info("User setup status checked: userId={}, hasCompletedSetup={}", userId, hasCompletedSetup);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking user setup status", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * JWT 토큰에서 사용자 정보 추출 (프론트엔드용)
     * 
     * @param token JWT 토큰
     * @return JWT 토큰에 포함된 사용자 정보
     */
    @GetMapping("/token/info")
    public ResponseEntity<?> getTokenInfo(
            @RequestHeader("Authorization") String token) {
        
        try {
            // JWT 토큰 검증
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token format"));
            }
            
            String jwt = token.substring(7);
            
            // JWT 토큰 유효성 검증
            if (!jwtUtil.validateToken(jwt)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }
            
            // JWT 토큰에서 사용자 정보 추출
            String providerId = jwtUtil.extractProviderId(jwt);
            String name = jwtUtil.extractName(jwt);
            String profileImage = jwtUtil.extractProfileImage(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("name", name);
            response.put("profileImage", profileImage);
            response.put("providerId", providerId);
            
            log.info("Token info extracted: userId={}, name={}", userId, name);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error extracting token info", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    
}
