package com.example.demo.dto;

import com.example.demo.entity.MajorCategory;
import com.example.demo.entity.MinorCategory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 사용자 카테고리 설정 요청을 위한 DTO
 * 프론트엔드에서 백엔드로 카테고리 선택 정보를 전송할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    
    /**
     * 사용자 ID (선택사항 - 인증된 사용자 정보에서 자동 설정)
     */
    private Long userId;
    
    /**
     * 선택된 카테고리 정보
     * Key: 대분류 (MajorCategory enum 값)
     * Value: 선택된 소분류 목록 (MinorCategory enum 값들)
     */
    private Map<MajorCategory, List<MinorCategory>> categories;
    
    /**
     * DTO 유효성 검사
     * @return 유효한 DTO인지 여부
     */
    public boolean isValid() {
        if (categories == null || categories.isEmpty()) {
            return false;
        }
        
        // 각 대분류에 대해 최소 하나의 소분류가 선택되어야 함
        return categories.values().stream()
                .allMatch(minorCategories -> 
                    minorCategories != null && !minorCategories.isEmpty()
                );
    }
    
    /**
     * 선택된 총 카테고리 수 반환
     * @return 선택된 소분류 카테고리 총 개수
     */
    public int getTotalSelectedCategories() {
        if (categories == null) return 0;
        
        return categories.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * 특정 대분류에 선택된 소분류가 있는지 확인
     * @param majorCategory 확인할 대분류
     * @return 선택된 소분류가 있는지 여부
     */
    public boolean hasSelectedCategories(MajorCategory majorCategory) {
        if (categories == null) return false;
        
        List<MinorCategory> minorCategories = categories.get(majorCategory);
        return minorCategories != null && !minorCategories.isEmpty();
    }
}
