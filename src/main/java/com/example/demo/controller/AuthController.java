package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import java.util.HashMap;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    // 카카오 OAuth2 리다이렉트 URI 처리 (카카오가 직접 호출)
    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<?> kakaoOAuth2Callback(@RequestParam("code") String code, @RequestParam(value = "state", required = false) String state) {
        try {
            System.out.println("=== 카카오 OAuth2 콜백 수신 ===");
            System.out.println("인가코드: " + code);
            System.out.println("state: " + state);
            
            // Spring Security OAuth2가 자동으로 처리하도록 리다이렉트
            // 실제로는 이 엔드포인트가 호출되지 않아야 하지만, 
            // 혹시 호출되는 경우를 대비해 로깅만 추가
            System.out.println("카카오 콜백 수신됨 - Spring Security OAuth2로 처리됨");
            
            // Spring Security OAuth2가 자동으로 처리하므로 여기서는 아무것도 하지 않음
            return ResponseEntity.ok(Map.of("message", "OAuth2 callback received"));
            
        } catch (Exception e) {
            System.out.println("카카오 OAuth2 콜백 처리 중 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // OAuth2 로그인 성공 처리 (Spring Security에서 자동 리다이렉트)
    @GetMapping("/auth/login/success")
    public ResponseEntity<?> oauth2LoginSuccess(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            System.out.println("=== OAuth2 로그인 성공 ===");
            System.out.println("OAuth2 사용자 정보: " + oauth2User.getAttributes());
            
            if (oauth2User != null) {
                // 카카오에서 동의받은 정보만 추출 (닉네임, 프로필 이미지)
                String username = extractUsername(oauth2User);
                String providerId = extractProviderId(oauth2User);
                String profileImage = extractProfileImage(oauth2User);
                
                System.out.println("=== 추출된 사용자 정보 ===");
                System.out.println("닉네임: " + username);
                System.out.println("제공자 ID: " + providerId);
                System.out.println("프로필 이미지: " + profileImage);
                
                // 사용자 정보를 데이터베이스에 저장하거나 업데이트
                User user = userService.createOrUpdateUser(username, profileImage, providerId);
                System.out.println("사용자 정보 저장/업데이트 완료: " + user.getUserId());
                
                // JWT 토큰 생성
                String jwtToken = jwtUtil.generateToken(providerId, username);
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", user.getUserId());
                userData.put("providerId", providerId);
                userData.put("username", username);
                userData.put("profileImage", profileImage);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("token", jwtToken);
                response.put("type", "Bearer");
                response.put("user", userData);
                response.put("message", "OAuth2 login successful");
                
                System.out.println("JWT 토큰 생성 완료: " + jwtToken);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
        } catch (Exception e) {
            System.out.println("OAuth2 로그인 성공 처리 중 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // OAuth2 로그인 실패 처리 (Spring Security에서 자동 리다이렉트)
    @GetMapping("/auth/login/failure")
    public ResponseEntity<?> oauth2LoginFailure() {
        System.out.println("=== OAuth2 로그인 실패 ===");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "OAuth2 login failed");
        
        return ResponseEntity.status(401).body(response);
    }

    // 인증 상태 확인 엔드포인트
    @GetMapping("/auth/status")
    public ResponseEntity<?> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", false);
        response.put("message", "Authentication status check");
        return ResponseEntity.ok(response);
    }

    // 사용자명 추출 (OAuth2User 버전)
    private String extractUsername(OAuth2User oauth2User) {
        try {
            Map<String, Object> attributes = oauth2User.getAttributes();
            System.out.println("전체 attributes: " + attributes);
            
            // 카카오 계정 정보에서 닉네임 추출
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null && profile.get("nickname") != null) {
                    return profile.get("nickname").toString();
                }
            }
            
            // properties에서 닉네임 추출 (대안)
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties != null && properties.get("nickname") != null) {
                return properties.get("nickname").toString();
            }
            
            // 기본값
            return "카카오사용자_" + System.currentTimeMillis();
        } catch (Exception e) {
            System.out.println("닉네임 추출 실패: " + e.getMessage());
            return "카카오사용자_" + System.currentTimeMillis();
        }
    }

    // 카카오 ID 추출 (OAuth2User 버전)
    private String extractProviderId(OAuth2User oauth2User) {
        try {
            Map<String, Object> attributes = oauth2User.getAttributes();
            if (attributes.get("id") != null) {
                return attributes.get("id").toString();
            }
            return "kakao_" + System.currentTimeMillis();
        } catch (Exception e) {
            System.out.println("제공자 ID 추출 실패: " + e.getMessage());
            return "kakao_" + System.currentTimeMillis();
        }
    }

    // 프로필 이미지 추출 (OAuth2User 버전)
    private String extractProfileImage(OAuth2User oauth2User) {
        try {
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // properties에서 프로필 이미지 추출
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties != null && properties.get("profile_image") != null) {
                return properties.get("profile_image").toString();
            }
            
            // kakao_account에서 프로필 이미지 추출 (대안)
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null && profile.get("profile_image_url") != null) {
                    return profile.get("profile_image_url").toString();
                }
            }
            
            return "default_profile.jpg";
        } catch (Exception e) {
            System.out.println("프로필 이미지 추출 실패: " + e.getMessage());
            return "default_profile.jpg";
        }
    }
}

