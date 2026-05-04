package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 정보를 저장하는 JPA 엔티티
 */
@Entity
@Table(name = "users") // 데이터베이스 테이블명 지정
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@Builder // Lombok: 빌더 패턴 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
public class User {
    
    /**
     * 내부 ID (Primary Key, DB용)
     * 자동 증가하는 값
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * 외부 노출용 사용자 ID (UUID)
     * API Gateway와 다른 서비스에서 사용
     */
    @Column(name = "user_id", unique = true, nullable = false, length = 36)
    private String userId;
    
    /**
     * 사용자 이름 (닉네임)
     * null이 될 수 없음
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 사용자 프로필 이미지 URL
     * null 가능 (기본 이미지 사용)
     */
    @Column(name = "profile_image")
    private String profileImage;
    
    /**
     * 제공자에서 제공하는 사용자 ID
     * 예: 카카오에서 제공하는 고유 ID
     */
    @Column(name = "provider_id")
    private String providerId;
    
    /**
     * 사용자가 제공받은 문제들의 난이도 레벨
     * 1: 초급, 2: 중급, 3: 고급
     */
    @Column(name = "difficulty_level")
    private Integer difficultyLevel;
    
    /**
     * 사용자 계정 생성 시간
     * 자동으로 현재 시간으로 설정
     */

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 사용자 정보 마지막 수정 시간
     * 수정할 때마다 자동으로 현재 시간으로 업데이트
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    

    
    /**
     * 엔티티가 데이터베이스에 저장되기 전에 실행되는 메서드
     * 생성 시간과 수정 시간을 현재 시간으로 설정
     * UUID 자동 생성
     */
    @PrePersist // 데이터베이스에 저장하기 전에 실행
    protected void onCreate() {
        if (userId == null) {
            userId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now(); // 현재 시간으로 설정
    }
    
    /**
     * 엔티티가 데이터베이스에서 업데이트되기 전에 실행되는 메서드
     * 수정 시간을 현재 시간으로 업데이트
     */
    @PreUpdate 
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // 현재 시간으로 설정
    }
    
    /**
     * 사용자가 선택한 카테고리 목록
     * One-to-Many 관계: 한 사용자는 여러 카테고리를 선택할 수 있음
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserCategory> userCategories = new ArrayList<>();
}
