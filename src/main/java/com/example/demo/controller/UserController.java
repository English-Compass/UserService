package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.UserCategoryService;
import com.example.demo.dto.CategoryRequestDto;
import com.example.demo.dto.CategoryResponseDto;
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
     * 사용자 로그아웃 처리
     * 
     * @return 로그아웃 성공 메시지
     * 
     * 주의: 이 엔드포인트는 단순히 응답만 반환합니다.
     * 실제 로그아웃 처리는 Spring Security의 기본 로그아웃 기능을 사용하거나,
     * 프론트엔드에서 JWT 토큰을 제거하는 방식으로 구현해야 합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        log.info("User logout requested"); // 로그 기록
        
        // Spring Security의 기본 로그아웃 처리
        // 실제로는 프론트엔드에서 JWT 토큰을 제거하는 로직이 필요
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
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
     * 사용자 난이도 설정 (테스트용 - 인증 비활성화)
     * 
     * @param request 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)
     * @return 난이도 설정 결과
     */
    @PostMapping("/difficulty")
    public ResponseEntity<Map<String, Object>> setUserDifficulty(
            @RequestBody Map<String, Integer> request) {
        
        try {
            // TODO: 테스트 완료 후 인증 로직 활성화
            // @AuthenticationPrincipal UserDetails userDetails
            // if (userDetails == null) {
            //     return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            // }
            
            Integer difficultyLevel = request.get("difficultyLevel");
            if (difficultyLevel == null || difficultyLevel < 1 || difficultyLevel > 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid difficulty level. Must be 1, 2, or 3"));
            }
            
            // 테스트용으로 고정된 사용자 ID 사용 (실제로는 userDetails에서 가져와야 함)
            Long testUserId = 1L; // TODO: 실제 사용자 ID로 변경
            
            User updatedUser = userService.setUserDifficultyLevel(testUserId, difficultyLevel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Difficulty level updated successfully");
            response.put("userId", updatedUser.getUserId());
            response.put("difficultyLevel", updatedUser.getDifficultyLevel());
            
            log.info("User difficulty updated: userId={}, level={}", updatedUser.getUserId(), updatedUser.getDifficultyLevel());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting user difficulty", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 난이도 조회 (테스트용 - 인증 비활성화)
     * 
     * @return 사용자 난이도 정보
     */
    @GetMapping("/difficulty")
    public ResponseEntity<Map<String, Object>> getUserDifficulty() {
        try {
            // TODO: 테스트 완료 후 인증 로직 활성화
            // @AuthenticationPrincipal UserDetails userDetails
            // if (userDetails == null) {
            //     return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            // }
            
            // 테스트용으로 고정된 사용자 ID 사용 (실제로는 userDetails에서 가져와야 함)
            Long testUserId = 1L; // TODO: 실제 사용자 ID로 변경
            
            Integer difficultyLevel = userService.getUserDifficultyLevel(testUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", testUserId);
            response.put("difficultyLevel", difficultyLevel);
            
            log.info("User difficulty retrieved: userId={}, level={}", testUserId, difficultyLevel);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user difficulty", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 카테고리 저장 (테스트용 - 인증 비활성화)
     * 
     * @param categoryRequest 카테고리 요청 DTO
     * @return 카테고리 저장 결과
     */
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponseDto> saveUserCategories(
            @RequestBody CategoryRequestDto categoryRequest) {
        
        try {
            // DTO 유효성 검사
            if (!categoryRequest.isValid()) {
                return ResponseEntity.badRequest()
                    .body(CategoryResponseDto.failure("유효하지 않은 카테고리 데이터입니다."));
            }
            
            // TODO: 테스트 완료 후 인증 로직 활성화
            // @AuthenticationPrincipal UserDetails userDetails
            // if (userDetails == null) {
            //     return ResponseEntity.status(401).body(CategoryResponseDto.failure("User not authenticated"));
            // }
            
            // 테스트용으로 고정된 사용자 ID 사용 (실제로는 userDetails에서 가져와야 함)
            Long testUserId = 1L; // TODO: 실제 사용자 ID로 변경
            
            // CategoryRequestDto를 UserCategoryService에서 기대하는 형태로 변환
            Map<String, java.util.List<String>> categories = new HashMap<>();
            categoryRequest.getCategories().forEach((majorCategory, minorCategories) -> {
                List<String> minorCategoryStrings = minorCategories.stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toList());
                categories.put(majorCategory.name(), minorCategoryStrings);
            });
            
            var savedCategories = userCategoryService.saveUserCategories(testUserId, categories);
            
            // CategoryResponseDto로 응답 생성
            CategoryResponseDto response = CategoryResponseDto.success(testUserId, categoryRequest.getCategories());
            response.setMessage("카테고리가 성공적으로 저장되었습니다. (저장된 개수: " + savedCategories.size() + ")");
            
            log.info("User categories saved: userId={}, count={}", testUserId, savedCategories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error saving user categories", e);
            return ResponseEntity.status(500)
                .body(CategoryResponseDto.failure("카테고리 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
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
            
            User user = userService.findByProviderId(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(user.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("categories", categories);
            
            log.info("User categories retrieved: userId={}, count={}", user.getUserId(), categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user categories", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * 사용자 설정 정보 조회 (난이도 + 카테고리)
     * 
     * @param userDetails Spring Security에서 제공하는 인증된 사용자 정보
     * @return 사용자 설정 정보
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getUserSettings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            
            User user = userService.findByProviderId(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Integer difficultyLevel = userService.getUserDifficultyLevel(user.getUserId());
            Map<String, java.util.List<String>> categories = userCategoryService.getUserCategories(user.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("difficultyLevel", difficultyLevel);
            response.put("categories", categories);
            
            log.info("User settings retrieved: userId={}, difficulty={}, categories={}", 
                    user.getUserId(), difficultyLevel, categories.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting user settings", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
