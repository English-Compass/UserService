package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.List;

/**
 * 사용자 설정 정보 수정을 위한 요청 DTO
 * 
 * 주요 기능:
 * - 난이도 레벨 수정
 * - 사용자 카테고리 수정
 * - 입력 데이터 유효성 검증
 * 
 * 주의: 사용자 이름과 프로필 이미지는 카카오 로그인 시 자동으로 동기화되므로
 * 별도로 수정할 수 없습니다.
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@Builder // Lombok: 빌더 패턴 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
public class UserSettingsRequestDto {
    
    /**
     * 사용자 난이도 레벨
     * - 1: 초급, 2: 중급, 3: 고급
     * - null 가능 (기존 값 유지)
     * - 1, 2, 3 중 하나여야 함
     */
    // @Min(value = 1, message = "난이도 레벨은 1 이상이어야 합니다")
    // @Max(value = 3, message = "난이도 레벨은 3 이하여야 합니다")
    private Integer difficultyLevel;
    
    /**
     * 사용자 카테고리 정보
     * - Map<String, List<String>> 형태
     * - Key: 대분류 카테고리명
     * - Value: 소분류 카테고리명 리스트
     * - null 가능 (기존 값 유지)
     */
    private Map<String, List<String>> categories;
    
    /**
     * DTO의 유효성을 검증하는 메서드
     * 
     * @return 유효한 데이터인지 여부
     */
    public boolean isValid() {
        // 난이도 레벨 검증 (null이 아닌 경우에만)
        if (difficultyLevel != null && (difficultyLevel < 1 || difficultyLevel > 3)) {
            return false;
        }
        
        // 카테고리 검증 (null이 아닌 경우에만)
        if (categories != null) {
            for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
                String majorCategory = entry.getKey();
                List<String> minorCategories = entry.getValue();
                
                // 대분류 카테고리명이 비어있으면 안됨
                if (majorCategory == null || majorCategory.trim().isEmpty()) {
                    return false;
                }
                
                // 소분류 카테고리 리스트가 null이거나 비어있으면 안됨
                if (minorCategories == null || minorCategories.isEmpty()) {
                    return false;
                }
                
                // 소분류 카테고리명들이 비어있으면 안됨
                for (String minorCategory : minorCategories) {
                    if (minorCategory == null || minorCategory.trim().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 난이도 레벨이 설정되어 있는지 확인
     * 
     * @return 난이도 레벨이 설정되어 있으면 true
     */
    public boolean hasDifficultyLevel() {
        return difficultyLevel != null;
    }
    
    /**
     * 카테고리가 설정되어 있는지 확인
     * 
     * @return 카테고리가 설정되어 있으면 true
     */
    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }
    
    /**
     * 수정할 데이터가 있는지 확인
     * 
     * @return 난이도 레벨이나 카테고리 중 하나라도 설정되어 있으면 true
     */
    public boolean hasAnyData() {
        return hasDifficultyLevel() || hasCategories();
    }
}
