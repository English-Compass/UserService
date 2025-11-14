package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * 애플리케이션 전역에서 발생하는 예외를 처리합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 모든 예외를 처리하는 핸들러
     * 
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", e.getMessage());
        response.put("type", e.getClass().getSimpleName());
        
        // 스택 트레이스 정보 (개발 환경에서만)
        if (log.isDebugEnabled()) {
            response.put("stackTrace", e.getStackTrace());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * IllegalArgumentException 처리
     * 
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * HttpRequestMethodNotSupportedException 처리
     * 지원하지 않는 HTTP 메서드로 요청이 들어온 경우
     * 
     * @param e 발생한 예외
     * @param request HTTP 요청 객체
     * @return 에러 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, 
            HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("Method not supported: method={}, supportedMethods={}, requestURI={}", 
                e.getMethod(), e.getSupportedMethods(), requestURI);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method Not Allowed");
        response.put("message", String.format("Request method '%s' is not supported. Supported methods: %s", 
                e.getMethod(), String.join(", ", e.getSupportedMethods())));
        response.put("type", e.getClass().getSimpleName());
        response.put("path", requestURI);
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }
}

