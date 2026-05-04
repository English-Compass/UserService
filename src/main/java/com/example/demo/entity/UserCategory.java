package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 선택한 카테고리 정보를 저장하는 JPA 엔티티
 */
@Entity
@Table(name = "user_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCategory {
    
    /**
     * 카테고리 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 사용자와의 관계 (Many-to-One)
     * 한 사용자는 여러 카테고리를 선택할 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 대분류 (예: 기술, 디자인, 마케팅)
     */
    @Column(name = "major_category", nullable = false)
    private String majorCategory;
    
    /**
     * 소분류 (예: 프로그래밍, UI/UX, 디지털마케팅)
     */
    @Column(name = "minor_category", nullable = false)
    private String minorCategory;
    
    /**
     * 카테고리 선택 시간
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 엔티티가 데이터베이스에 저장되기 전에 실행되는 메서드
     * 생성 시간을 현재 시간으로 설정
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
