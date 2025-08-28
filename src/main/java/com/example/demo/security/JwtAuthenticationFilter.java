package com.example.demo.security;

import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰을 검증하고 인증을 처리하는 필터
 * 모든 HTTP 요청에 대해 JWT 토큰을 확인하고 인증 상태를 설정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Authorization 헤더에서 JWT 토큰 추출
            final String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // "Bearer " 제거하고 토큰만 추출
            final String jwt = authHeader.substring(7);
            
            // JWT 토큰 검증
            if (jwtUtil.validateToken(jwt)) {
                // 토큰에서 사용자명(providerId) 추출
                final String providerId = jwtUtil.extractUsername(jwt);
                
                // 사용자 정보 로드
                UserDetails userDetails = userDetailsService.loadUserByUsername(providerId);
                
                if (userDetails != null) {
                    // 인증 토큰 생성
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    // 요청 정보 설정
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT 인증 성공: {}", providerId);
                }
            } else {
                log.debug("JWT 토큰이 유효하지 않음");
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 필터 처리 중 오류 발생", e);
        }
        
        filterChain.doFilter(request, response);
    }
}
