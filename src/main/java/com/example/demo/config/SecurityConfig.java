package com.example.demo.config;

import com.example.demo.service.KakaoOAuth2UserService;
import com.example.demo.util.JwtUtil;
import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security 설정 클래스
 * 
 * 주요 기능:
 * - OAuth2 로그인 설정
 * - 보안 규칙 설정
 * - 인증/인가 처리
 * - 로그인 성공/실패 핸들러 설정
 */
@Configuration // Spring 설정 클래스임을 명시
@EnableWebSecurity // Spring Security 활성화
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 log 필드 자동 생성
public class SecurityConfig {
    
    /**
     * 사용자 인증 정보를 제공하는 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserDetailsService userDetailsService;
    
    /**
     * OAuth2 사용자 정보를 처리하는 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final KakaoOAuth2UserService oauth2UserService;
    
    /**
     * JWT 토큰 생성을 위한 유틸리티
     * 생성자 주입으로 의존성 주입
     */
    private final JwtUtil jwtUtil;
    
    /**
     * Spring Security 필터 체인 설정
     * 
     * @param http HTTP 보안 설정을 위한 HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security..."); // 로그 기록
        
        http
            // CSRF 보호 비활성화 (OAuth2 로그인을 위해)
            .csrf(AbstractHttpConfigurer::disable)
            
            // HTTP 기본 인증 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 폼 로그인 비활성화 (OAuth2만 사용)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            
            // JWT 인증 필터 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService), UsernamePasswordAuthenticationFilter.class)
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService) // 커스텀 OAuth2UserService 사용
                )
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, 
                                                     HttpServletResponse response, 
                                                     Authentication authentication) 
                            throws IOException, ServletException {
                        
                        log.info("=== OAuth2 로그인 성공 핸들러 실행 ===");
                        log.info("인증된 사용자: {}", authentication.getName());
                        
                        // JWT 토큰 생성 - Authentication에서 providerId와 name 추출
                        String providerId = authentication.getName(); // providerId
                        String name = authentication.getName(); // 기본값으로 providerId 사용
                        
                        // CustomUserDetails에서 실제 사용자 정보 가져오기
                        if (authentication.getPrincipal() instanceof com.example.demo.security.CustomUserDetails) {
                            com.example.demo.security.CustomUserDetails userDetails = 
                                (com.example.demo.security.CustomUserDetails) authentication.getPrincipal();
                            name = userDetails.getUsername(); // 사용자 이름
                        }
                        
                        String token = jwtUtil.generateToken(providerId, name);
                        log.info("JWT 토큰 생성 완료: {}", token.substring(0, Math.min(token.length(), 50)) + "...");
                        
                        // JWT 토큰을 쿼리 파라미터로 홈으로 리다이렉트
                        String redirectUrl = "/?token=" + token;
                        response.sendRedirect(redirectUrl);
                    }
                })
                .failureUrl("/auth/login/failure") // 로그인 실패 시 /auth/login/failure로 리다이렉트
            )
            
            // URL별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 접근 가능한 URL
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                // OAuth2 관련 URL (Spring Security가 자동 처리)
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // 인증 관련 URL은 공개 접근 가능
                .requestMatchers("/auth/**").permitAll()
                // 테스트용 사용자 API는 공개 접근 가능 (TODO: 테스트 완료 후 인증 필요로 변경)
                .requestMatchers("/user/difficulty").permitAll()
                .requestMatchers("/user/categories").permitAll()
                // 기타 사용자 관련 URL은 인증 필요
                .requestMatchers("/user/**").authenticated()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );
        
        log.info("Spring Security configuration completed"); // 로그 기록
        return http.build();
    }
}
