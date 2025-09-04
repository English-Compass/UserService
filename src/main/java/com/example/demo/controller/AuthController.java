package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

/**
 * 인증 관련 컨트롤러
 * 
 * 주요 기능:
 * - 인증 상태 확인
 * - OAuth2 로그인은 SecurityConfig에서 처리됨
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    /**
     * 인증 상태 확인 엔드포인트
     * 
     * @return 인증 상태 정보
     */
    @GetMapping("/auth/status")
    public ResponseEntity<?> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", false);
        response.put("message", "Authentication status check");
        return ResponseEntity.ok(response);
    }
}

