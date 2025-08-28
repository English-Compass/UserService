package com.example.demo.service;

import com.example.demo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * 카카오 OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 * Spring Security의 OAuth2UserService를 구현하여 카카오 로그인 처리
 * 
 * 주요 기능:
 * - 카카오 OAuth2 사용자 정보 로드
 * - 사용자 정보를 내부 시스템에 저장/업데이트
 * - Spring Security 인증 객체 생성
 */
@Service // Spring이 이 클래스를 서비스 계층의 컴포넌트로 인식
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 log 필드 자동 생성
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    /**
     * 사용자 관련 비즈니스 로직을 처리하는 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserService userService;
    
    /**
     * 카카오 OAuth2 사용자 정보를 로드하고 처리
     * 
     * @param userRequest OAuth2 사용자 요청 정보
     * @return Spring Security에서 사용할 수 있는 OAuth2User 객체
     * @throws OAuth2AuthenticationException OAuth2 인증 중 발생하는 예외
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== 카카오 OAuth2 사용자 정보 로드 시작 ===");
        log.info("클라이언트 등록 ID: {}", userRequest.getClientRegistration().getRegistrationId());
        log.info("스코프: {}", userRequest.getClientRegistration().getScopes());
        
        try {
            // 기본 OAuth2 사용자 서비스를 사용하여 사용자 정보 로드
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oauth2User = delegate.loadUser(userRequest);
            
            log.info("카카오 OAuth2 사용자 정보 로드 완료: {}", oauth2User.getName());
            log.info("카카오 OAuth2 사용자 속성: {}", oauth2User.getAttributes());
            
            // OAuth2 제공자 정보 가져오기
            String provider = userRequest.getClientRegistration().getRegistrationId();
            log.info("OAuth2 제공자: {}", provider);
            
            // 제공자별로 사용자 정보 처리
            if ("kakao".equals(provider)) {
                log.info("카카오 사용자 처리 시작");
                OAuth2User processedUser = processKakaoUser(oauth2User);
                log.info("카카오 사용자 처리 완료: {}", processedUser.getName());
                return processedUser;
            } else {
                log.warn("지원하지 않는 OAuth2 제공자: {}", provider);
                return oauth2User;
            }
            
        } catch (Exception e) {
            log.error("카카오 OAuth2 사용자 로드 중 에러 발생", e);
            OAuth2Error oauth2Error = new OAuth2Error("oauth2_error", "Failed to load OAuth2 user", null);
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }
    
    /**
     * 카카오 사용자 정보를 처리
     * 
     * @param oauth2User OAuth2에서 제공하는 사용자 정보
     * @return 처리된 OAuth2User 객체
     */
    private OAuth2User processKakaoUser(OAuth2User oauth2User) {
        log.info("=== 카카오 사용자 정보 처리 시작 ===");
        log.info("카카오 사용자 이름: {}", oauth2User.getName());
        log.info("카카오 사용자 속성 전체: {}", oauth2User.getAttributes());
        
        // 카카오 사용자 정보에서 필요한 데이터 추출
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // 전체 카카오 응답 데이터 로깅 (디버깅용)
        log.info("=== 카카오 전체 응답 데이터 ===");
        log.info("전체 attributes: {}", attributes);
        
        // 카카오 계정 정보
        Long kakaoId = (Long) attributes.get("id");
        String email = extractEmail(attributes);
        String nickname = extractNickname(attributes);
        String profileImage = extractProfileImage(attributes);
        
        log.info("=== 추출된 카카오 사용자 정보 ===");
        log.info("  - 카카오 ID: {}", kakaoId);
        log.info("  - 이메일: {}", email);
        log.info("  - 닉네임: {}", nickname);
        log.info("  - 프로필 이미지: {}", profileImage);
        
        // 사용자 정보를 데이터베이스에 저장하거나 업데이트
        log.info("UserService.createOrUpdateUser() 호출 시작");
        User user = userService.createOrUpdateUser(
            nickname != null ? nickname : "카카오사용자",
            profileImage,
            kakaoId.toString()
        );
        
        log.info("사용자 정보 저장/업데이트 완료:");
        log.info("  - 사용자 ID: {}", user.getUserId());
        log.info("  - 이름: {}", user.getName());
        log.info("  - 제공자 ID: {}", user.getProviderId());
        
        // Spring Security에서 사용할 수 있는 OAuth2User 객체 생성
        log.info("Spring Security OAuth2User 객체 생성 시작");
        DefaultOAuth2User oauth2UserForSecurity = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("USER")),
            attributes,
            "id" // 카카오에서 사용자 식별자로 사용할 속성명
        );
        
        log.info("OAuth2User 객체 생성 완료: {}", oauth2UserForSecurity.getName());
        log.info("=== 카카오 사용자 정보 처리 완료 ===");
        
        return oauth2UserForSecurity;
    }
    
    /**
     * 카카오 사용자 정보에서 이메일 추출
     * 
     * @param attributes 카카오에서 제공하는 사용자 속성
     * @return 추출된 이메일 주소 (없으면 null)
     */
    private String extractEmail(Map<String, Object> attributes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null) {
                return (String) kakaoAccount.get("email");
            }
        } catch (Exception e) {
            log.warn("Failed to extract email from Kakao user attributes", e); // 경고 로그
        }
        return null;
    }
    
    /**
     * 카카오 사용자 정보에서 닉네임 추출
     * 
     * @param attributes 카카오에서 제공하는 사용자 속성
     * @return 추출된 닉네임 (없으면 null)
     */
    private String extractNickname(Map<String, Object> attributes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties != null) {
                return (String) properties.get("nickname");
            }
        } catch (Exception e) {
            log.warn("Failed to extract nickname from Kakao user attributes", e); // 경고 로그
        }
        return null;
    }
    
    /**
     * 카카오 사용자 정보에서 프로필 이미지 URL 추출
     * 
     * @param attributes 카카오에서 제공하는 사용자 속성
     * @return 추출된 프로필 이미지 URL (없으면 null)
     */
    private String extractProfileImage(Map<String, Object> attributes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            if (properties != null) {
                return (String) properties.get("profile_image");
            }
        } catch (Exception e) {
            log.warn("Failed to extract profile image from Kakao user attributes", e); // 경고 로그
        }
        return null;
    }
}
