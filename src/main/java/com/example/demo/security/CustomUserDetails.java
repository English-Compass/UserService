package com.example.demo.security;

import com.example.demo.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security에서 사용할 UserDetails 구현체
 * User 엔티티를 래핑하여 인증/인가에 필요한 정보를 제공
 */
@Getter
public class CustomUserDetails implements UserDetails {
    
    private final User user;
    
    public CustomUserDetails(User user) {
        this.user = user;
    }
    
    /**
     * Spring Security에서 사용자의 권한을 반환
     * User 엔티티의 역할(Role)을 권한으로 변환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("USER"));
    }
    
    /**
     * Spring Security에서 사용자의 비밀번호를 반환
     * OAuth2 사용자는 비밀번호가 없으므로 null 반환
     */
    @Override
    public String getPassword() {
        return null; // OAuth2 사용자는 비밀번호가 없음
    }
    
    /**
     * Spring Security에서 사용자의 사용자명을 반환
     * User 엔티티의 name을 사용자명으로 사용
     */
    @Override
    public String getUsername() {
        return user.getName(); // name을 사용자명으로 사용
    }
    
    /**
     * 계정이 만료되지 않았는지 확인
     * 현재는 항상 true 반환 (만료 기능 미구현)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    /**
     * 계정이 잠기지 않았는지 확인
     * 현재는 항상 true 반환 (계정 잠금 기능 미구현)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    /**
     * 비밀번호가 만료되지 않았는지 확인
     * OAuth2 사용자는 비밀번호가 없으므로 항상 true 반환
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    /**
     * 계정이 활성화되어 있는지 확인
     * 현재는 항상 true 반환 (계정 비활성화 기능 미구현)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * User 엔티티의 ID를 반환
     */
    public Long getUserId() {
        return user.getUserId();
    }
    
    /**
     * User 엔티티의 이름을 반환
     */
    public String getName() {
        return user.getName();
    }
    
    /**
     * User 엔티티의 프로필 이미지를 반환
     */
    public String getProfileImage() {
        return user.getProfileImage();
    }
    
    /**
     * User 엔티티의 제공자 ID를 반환
     */
    public String getProviderId() {
        return user.getProviderId();
    }
}