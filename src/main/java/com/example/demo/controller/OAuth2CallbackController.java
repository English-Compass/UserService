package com.example.demo.controller;

import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 콜백 처리를 위한 컨트롤러
 * 
 * API Gateway를 통해 JWT 토큰이 포함된 요청을 처리합니다.
 * 이 컨트롤러는 OAuth2 필터보다 먼저 실행되어 JWT 토큰이 포함된 요청을 처리합니다.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class OAuth2CallbackController {
    
    /**
     * JWT 토큰 유틸리티
     */
    private final JwtUtil jwtUtil;
    
    /**
     * 프론트엔드 기본 URL (환경변수로 설정 가능)
     */
    @Value("${FRONTEND_BASE_URL:http://localhost:5173/}")
    private String frontendBaseUrl;
    
    /**
     * OAuth2 콜백 요청 처리
     * 
     * 쿠키에서 JWT 토큰을 읽고 사용자 정보를 JSON으로 반환합니다.
     * 
     * @param redirect 리다이렉트할 프론트엔드 URL
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 사용자 정보를 포함한 JSON 응답 또는 리다이렉트
     * @throws IOException 리다이렉트 중 발생할 수 있는 예외
     */
    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<Map<String, Object>> handleOAuth2Callback(
            @RequestParam(required = false) String redirect,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("OAuth2 콜백 요청 수신");
        
        // 쿠키에서 JWT 토큰 읽기
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    log.info("쿠키에서 JWT 토큰 읽기 성공");
                    break;
                }
            }
        }
        
        // 쿠키에 토큰이 있는 경우: 사용자 정보를 JSON으로 반환
        if (token != null && !token.isEmpty()) {
            try {
                // JWT 토큰에서 사용자 정보 추출
                String userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractName(token);
                String profileImage = jwtUtil.extractProfileImage(token);
                
                log.info("JWT 토큰에서 사용자 정보 추출: userId={}, username={}, profileImage={}", 
                        userId, username, profileImage);
                
                // 사용자 정보를 JSON으로 반환
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", userId);
                userInfo.put("name", username);
                userInfo.put("profileImage", profileImage);
                userInfo.put("success", true);
                
                // redirect 파라미터가 있으면 함께 반환
                if (redirect != null && !redirect.isEmpty()) {
                    userInfo.put("redirect", redirect);
                } else {
                    String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
                    userInfo.put("redirect", baseUrl + "dashboard/home");
                }
                
                log.info("사용자 정보 반환: {}", userInfo);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(userInfo);
                
            } catch (Exception e) {
                log.error("JWT 토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Failed to extract user information from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(error);
            }
        }
        
        // 쿠키에 토큰이 없는 경우: 정상적인 OAuth2 플로우 (카카오에서 직접 온 요청)
        // 이 경우는 Spring Security OAuth2 필터가 처리하도록 함
        log.info("쿠키에 JWT 토큰이 없는 요청 - Spring Security OAuth2 필터로 위임");
        // Spring Security OAuth2 필터가 처리하도록 하기 위해 200 OK 반환
        // (실제로는 필터가 리다이렉트를 처리함)
        return ResponseEntity.ok().build();
    }
}

