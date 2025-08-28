package com.example.demo.dto;

import com.example.demo.entity.MajorCategory;
import com.example.demo.entity.MinorCategory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 사용자 카테고리 정보 응답을 위한 DTO
 * 백엔드에서 프론트엔드로 카테고리 정보를 반환할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 사용자 카테고리 정보
     * Key: 대분류 (MajorCategory enum 값)
     * Value: 선택된 소분류 목록 (MinorCategory enum 값들)
     */
    private Map<MajorCategory, List<MinorCategory>> categories;
    
    /**
     * 카테고리 설정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 선택된 총 카테고리 수
     */
    private int totalCategories;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 성공 여부
     */
    private boolean success;
    
    /**
     * 성공 응답 생성
     */
    public static CategoryResponseDto success(Long userId, Map<MajorCategory, List<MinorCategory>> categories) {
        CategoryResponseDto response = new CategoryResponseDto();
        response.setUserId(userId);
        response.setCategories(categories);
        response.setUpdatedAt(LocalDateTime.now());
        response.setTotalCategories(calculateTotalCategories(categories));
        response.setMessage("카테고리가 성공적으로 저장되었습니다.");
        response.setSuccess(true);
        return response;
    }
    
    /**
     * 실패 응답 생성
     */
    public static CategoryResponseDto failure(String message) {
        CategoryResponseDto response = new CategoryResponseDto();
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
    
    /**
     * 총 카테고리 수 계산
     */
    private static int calculateTotalCategories(Map<MajorCategory, List<MinorCategory>> categories) {
        if (categories == null) return 0;
        
        return categories.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
