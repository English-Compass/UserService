package com.example.demo.security;

import com.example.demo.service.UserService;
import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 인증 필터
 * Authorization 헤더에서 JWT 토큰을 추출하여 인증 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Authorization 헤더에서 JWT 토큰 추출
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    // JWT 토큰 유효성 검증
                    if (jwtUtil.validateToken(token)) {
                        // JWT 토큰에서 providerId 추출
                        String providerId = jwtUtil.extractProviderId(token);
                        
                        if (providerId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            // UserService를 통해 사용자 정보 로드
                            userService.findByProviderId(providerId).ifPresent(user -> {
                                CustomUserDetails userDetails = new CustomUserDetails(user);
                                
                                // 인증 토큰 생성
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails, 
                                        null, 
                                        userDetails.getAuthorities()
                                    );
                                
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                
                                // SecurityContext에 인증 정보 설정
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                
                                log.debug("JWT 인증 성공: providerId={}, userId={}", providerId, user.getUserId());
                            });
                        }
                    } else {
                        log.debug("JWT 토큰이 유효하지 않음");
                    }
                } catch (Exception e) {
                    log.debug("JWT 토큰 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 필터에서 오류 발생", e);
        }
        
        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}

