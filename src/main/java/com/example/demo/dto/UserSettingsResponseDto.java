package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 설정 정보 응답을 위한 DTO
 * 
 * 주요 기능:
 * - 사용자 설정 정보 조회 응답
 * - 설정 수정 결과 응답
 * - 성공/실패 상태 관리
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@Builder // Lombok: 빌더 패턴 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
public class UserSettingsResponseDto {
    
    /**
     * 응답 성공 여부
     */
    private boolean success;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 사용자 이름 (닉네임)
     */
    private String name;
    
    /**
     * 사용자 프로필 이미지 URL
     */
    private String profileImage;
    
    /**
     * 사용자 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)
     */
    private Integer difficultyLevel;
    
    /**
     * 사용자 계정 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 사용자 정보 마지막 수정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 추가 데이터를 담을 수 있는 맵
     */
    private Map<String, Object> additionalData;
    
    /**
     * 성공 응답을 생성하는 정적 메서드
     * 
     * @param userId 사용자 ID
     * @param name 사용자 이름
     * @param profileImage 프로필 이미지 URL
     * @param difficultyLevel 난이도 레벨
     * @param createdAt 생성 시간
     * @param updatedAt 수정 시간
     * @return 성공 응답 DTO
     */
    public static UserSettingsResponseDto success(Long userId, String name, String profileImage, 
                                                 Integer difficultyLevel, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return UserSettingsResponseDto.builder()
                .success(true)
                .message("사용자 설정 정보가 성공적으로 조회되었습니다")
                .userId(userId)
                .name(name)
                .profileImage(profileImage)
                .difficultyLevel(difficultyLevel)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .additionalData(new HashMap<>())
                .build();
    }
    
    /**
     * 설정 수정 성공 응답을 생성하는 정적 메서드
     * 
     * @param userId 사용자 ID
     * @param name 사용자 이름
     * @param profileImage 프로필 이미지 URL
     * @param difficultyLevel 난이도 레벨
     * @param createdAt 생성 시간
     * @param updatedAt 수정 시간
     * @return 설정 수정 성공 응답 DTO
     */
    public static UserSettingsResponseDto updateSuccess(Long userId, String name, String profileImage, 
                                                       Integer difficultyLevel, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return UserSettingsResponseDto.builder()
                .success(true)
                .message("사용자 설정이 성공적으로 수정되었습니다")
                .userId(userId)
                .name(name)
                .profileImage(profileImage)
                .difficultyLevel(difficultyLevel)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .additionalData(new HashMap<>())
                .build();
    }
    
    /**
     * 실패 응답을 생성하는 정적 메서드
     * 
     * @param message 에러 메시지
     * @return 실패 응답 DTO
     */
    public static UserSettingsResponseDto failure(String message) {
        return UserSettingsResponseDto.builder()
                .success(false)
                .message(message)
                .additionalData(new HashMap<>())
                .build();
    }
    
    /**
     * 추가 데이터를 설정하는 메서드
     * 
     * @param key 키
     * @param value 값
     * @return 현재 DTO 인스턴스 (메서드 체이닝)
     */
    public UserSettingsResponseDto addData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
        return this;
    }
    
    /**
     * 응답을 Map 형태로 변환하는 메서드
     * 
     * @return Map 형태의 응답 데이터
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        
        if (userId != null) {
            map.put("userId", userId);
        }
        if (name != null) {
            map.put("name", name);
        }
        if (profileImage != null) {
            map.put("profileImage", profileImage);
        }
        if (difficultyLevel != null) {
            map.put("difficultyLevel", difficultyLevel);
        }
        if (createdAt != null) {
            map.put("createdAt", createdAt);
        }
        if (updatedAt != null) {
            map.put("updatedAt", updatedAt);
        }
        if (additionalData != null) {
            map.putAll(additionalData);
        }
        
        return map;
    }
}
