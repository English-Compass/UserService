package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.service.KakaoOAuth2UserService;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
     * JWT 인증 필터
     * 생성자 주입으로 의존성 주입
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 프론트엔드 기본 URL (환경변수로 설정 가능)
     * 기본값: http://localhost:5173/
     */
    @Value("${FRONTEND_BASE_URL:http://localhost:5173/}")
    private String frontendBaseUrl;
    
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
            // CORS 설정 (프론트엔드에서 백엔드로 요청 허용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // JWT 인증 필터 추가 (OAuth2 로그인 필터 전에 실행)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
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
            
            // OAuth2 로그인 설정
            // JWT 검증은 API Gateway에서 처리하므로 여기서는 제거됨
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
                        
                        // OAuth2User에서 실제 사용자 정보 가져오기
                        String providerId = authentication.getName(); // 기본값
                        String username = "Unknown"; // 기본값
                        String profileImage = "default_profile.jpg"; // 기본값
                        String userId = null;  // UUID (String)
                        
                        log.info("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());
                        
                        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                                (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                            
                            // OAuth2User의 모든 attributes 로깅
                            log.info("OAuth2User attributes 전체: {}", oauth2User.getAttributes());
                            
                            // KakaoOAuth2UserService에서 추가한 사용자 정보 가져오기
                            userId = (String) oauth2User.getAttributes().get("_user_id");  // UUID (String)
                            username = (String) oauth2User.getAttributes().get("_user_name");
                            profileImage = (String) oauth2User.getAttributes().get("_user_profile_image");
                            providerId = (String) oauth2User.getAttributes().get("_provider_id");
                            
                            log.info("OAuth2User에서 정보 추출: userId={}, username={}, profileImage={}, providerId={}", 
                                    userId, username, profileImage, providerId);
                        } else {
                            log.warn("OAuth2User가 아닌 타입: {}", authentication.getPrincipal().getClass().getName());
                        }
                        
                        String token = jwtUtil.generateTokenWithUserInfo(providerId, username, userId, profileImage);
                        log.info("JWT 토큰 생성 완료: {}", token.substring(0, Math.min(token.length(), 50)) + "...");
                        
                        // JWT 토큰을 HttpOnly 쿠키에 저장 (보안 강화)
                        // 환경에 따라 Secure 설정 (로컬 개발: false, 프로덕션: true)
                        String secureFlag = System.getenv("JWT_COOKIE_SECURE");
                        boolean isSecure = secureFlag != null && "true".equalsIgnoreCase(secureFlag);
                        
                        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("access_token", token);
                        jwtCookie.setHttpOnly(true); // XSS 공격 방지
                        jwtCookie.setSecure(isSecure); // 환경에 따라 HTTPS에서만 전송 (프로덕션 환경)
                        jwtCookie.setPath("/"); // 모든 경로에서 쿠키 사용 가능
                        jwtCookie.setMaxAge(24 * 60 * 60); // 24시간 (초 단위)
                        
                        // SameSite 설정 (CSRF 공격 방지)
                        // Secure가 true일 때만 SameSite=None 사용 가능, 그렇지 않으면 Lax 사용
                        String sameSite = isSecure ? "SameSite=None" : "SameSite=Lax";
                        
                        // Domain 설정: localhost에서 모든 포트에서 쿠키 공유
                        // 프로덕션에서는 실제 도메인으로 설정 (예: .example.com)
                        String cookieDomain = System.getenv("JWT_COOKIE_DOMAIN");
                        if (cookieDomain == null || cookieDomain.isEmpty()) {
                            cookieDomain = "localhost"; // 로컬 개발 환경
                        }
                        
                        String cookieHeader = String.format("%s=%s; HttpOnly; Path=/; Max-Age=%d; Domain=%s; %s",
                                jwtCookie.getName(), jwtCookie.getValue(), jwtCookie.getMaxAge(), cookieDomain, sameSite);
                        if (isSecure) {
                            cookieHeader += "; Secure";
                        }
                        response.addHeader("Set-Cookie", cookieHeader);
                        log.info("JWT 토큰을 HttpOnly 쿠키에 저장 완료 (Secure: {})", isSecure);
                        
                        // 프론트엔드로 직접 리다이렉트 (쿠키는 브라우저에 저장되어 있음)
                        // 프론트엔드에서 /login/oauth2/code/kakao API를 호출하여 사용자 정보를 가져옴
                        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
                        String redirectPath;
                        
                        if (userId != null) {
                            // 기존 사용자
                            redirectPath = baseUrl + "dashboard/home";
                        } else {
                            // 새로운 사용자
                            redirectPath = baseUrl + "add-info";
                        }
                        
                        // 프론트엔드로 직접 리다이렉트
                        // 프론트엔드에서 쿠키를 읽고 /login/oauth2/code/kakao API를 호출하여 사용자 정보를 가져옴
                        // 로그인 성공 플래그를 추가하여 프론트엔드가 API를 호출하도록 유도
                        String redirectUrlWithFlag = redirectPath + (redirectPath.contains("?") ? "&" : "?") + "oauth2_success=true";
                        log.info("프론트엔드로 리다이렉트: {}", redirectUrlWithFlag);
                        response.sendRedirect(redirectUrlWithFlag);
                        log.info("프론트엔드로 리다이렉트 완료");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    // JWT 토큰이 포함된 요청은 API Gateway를 통해 온 요청이므로 API Gateway로 리다이렉트
                    String token = request.getParameter("token");
                    if (token != null && !token.isEmpty()) {
                        log.info("JWT 토큰이 포함된 요청 - API Gateway로 리다이렉트 (JWT 토큰 검증을 위해)");
                        
                        // 쿼리 파라미터에서 정보 가져오기 (없으면 JWT 토큰에서 추출)
                        String userId = request.getParameter("userId");
                        String username = request.getParameter("username");
                        String profileImage = request.getParameter("profileImage");
                        String redirect = request.getParameter("redirect");
                        
                        try {
                            if (userId == null || userId.isEmpty()) {
                                userId = jwtUtil.extractUserId(token);
                                log.info("JWT 토큰에서 userId 추출: {}", userId);
                            }
                            if (username == null || username.isEmpty()) {
                                username = jwtUtil.extractName(token);
                                log.info("JWT 토큰에서 username 추출: {}", username);
                            }
                            if (profileImage == null || profileImage.isEmpty()) {
                                profileImage = jwtUtil.extractProfileImage(token);
                                log.info("JWT 토큰에서 profileImage 추출: {}", profileImage);
                            }
                        } catch (Exception e) {
                            log.warn("JWT 토큰에서 정보 추출 실패: {}", e.getMessage());
                        }
                        
                        String redirectUri = System.getenv("OAUTH2_REDIRECT_URI");
                        if (redirectUri == null || redirectUri.isEmpty()) {
                            redirectUri = "http://localhost:8080/api/auth/oauth2/callback/kakao";
                        }
                        
                        // 모든 정보를 포함하여 API Gateway로 리다이렉트
                        StringBuilder redirectUrl = new StringBuilder(redirectUri);
                        redirectUrl.append("?token=").append(URLEncoder.encode(token, StandardCharsets.UTF_8));
                        
                        if (userId != null && !userId.isEmpty()) {
                            redirectUrl.append("&userId=").append(URLEncoder.encode(userId, StandardCharsets.UTF_8));
                        }
                        if (username != null && !username.isEmpty()) {
                            redirectUrl.append("&username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
                        }
                        if (profileImage != null && !profileImage.isEmpty()) {
                            redirectUrl.append("&profileImage=").append(URLEncoder.encode(profileImage, StandardCharsets.UTF_8));
                        }
                        if (redirect != null && !redirect.isEmpty()) {
                            redirectUrl.append("&redirect=").append(URLEncoder.encode(redirect, StandardCharsets.UTF_8));
                        } else {
                            String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
                            redirectUrl.append("&redirect=").append(URLEncoder.encode(baseUrl + "dashboard/home", StandardCharsets.UTF_8));
                        }
                        
                        log.info("API Gateway로 리다이렉트 (JWT 토큰 검증): userId={}, username={}, profileImage={}", 
                                userId, username, profileImage);
                        log.info("API Gateway로 리다이렉트 URL: {}", redirectUrl.toString());
                        response.sendRedirect(redirectUrl.toString());
                        return;
                    }
                    
                    // 실제 OAuth2 실패 처리
                    log.error("OAuth2 로그인 실패: {}", exception.getMessage());
                    String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
                    String redirectUrl = baseUrl + "?login=error&message=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
                    response.sendRedirect(redirectUrl);
                })
            )
            
            // URL별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 접근 가능한 URL
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
                // OAuth2 관련 URL (Spring Security가 자동 처리)
                // API Gateway를 통한 OAuth2 콜백 경로도 허용
                .requestMatchers("/api/auth/oauth2/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // 에러 페이지는 공개 접근 가능 (리다이렉션 루프 방지)
                .requestMatchers("/error").permitAll()
                // 테스트용 사용자 API는 공개 접근 가능 (TODO: 테스트 완료 후 인증 필요로 변경)
                .requestMatchers("/user/settings/difficulty").permitAll()
                .requestMatchers("/user/settings/categories").permitAll()
                // 사용자 설정 및 정보 조회는 인증 필요 (Spring Security 인증 사용)
                .requestMatchers("/user/settings/status").authenticated()
                .requestMatchers("/user/token/info").authenticated()
                // 기타 사용자 관련 URL은 인증 필요
                .requestMatchers("/user/**").authenticated()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );
        
        log.info("Spring Security configuration completed"); // 로그 기록
        return http.build();
    }
    
    /**
     * CORS 설정 (프론트엔드에서 백엔드로 요청 허용)
     * 
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프론트엔드 URL 허용
        configuration.addAllowedOrigin(frontendBaseUrl.replaceAll("/$", ""));
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("http://localhost:3000");
        
        // 허용할 HTTP 메서드
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        
        // 허용할 헤더
        configuration.addAllowedHeader("*");
        
        // 인증 정보 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
