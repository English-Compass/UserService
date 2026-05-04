package com.example.demo.entity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 대분류와 소분류 카테고리 간의 매핑을 제공하는 유틸리티 클래스
 */
public class CategoryMapper {
    
    /**
     * 대분류별 소분류 매핑
     * 
     * @param majorCategory 대분류
     * @return 해당 대분류에 속하는 소분류 배열
     */
    public static MinorCategory[] getMinorCategoriesByMajor(MajorCategory majorCategory) {
        switch (majorCategory) {
            case STUDY:
                return new MinorCategory[]{
                    MinorCategory.CLASS_LISTENING, 
                    MinorCategory.DEPARTMENT_CONVERSATION, 
                    MinorCategory.ASSIGNMENT_EXAM
                };
            case BUSINESS:
                return new MinorCategory[]{
                    MinorCategory.MEETING_CONFERENCE, 
                    MinorCategory.CUSTOMER_SERVICE, 
                    MinorCategory.EMAIL_REPORT
                };
            case TRAVEL:
                return new MinorCategory[]{
                    MinorCategory.BACKPACKING, 
                    MinorCategory.FAMILY_TRIP, 
                    MinorCategory.FRIEND_TRIP
                };
            case DAILY_LIFE:
                return new MinorCategory[]{
                    MinorCategory.SHOPPING_DINING, 
                    MinorCategory.HOSPITAL_VISIT, 
                    MinorCategory.PUBLIC_TRANSPORT
                };
            default:
                return new MinorCategory[0];
        }
    }
    
    /**
     * 대분류별 소분류 매핑 (List 형태로 반환)
     * 
     * @param majorCategory 대분류
     * @return 해당 대분류에 속하는 소분류 List
     */
    public static List<MinorCategory> getMinorCategoriesListByMajor(MajorCategory majorCategory) {
        return Arrays.asList(getMinorCategoriesByMajor(majorCategory));
    }
    
    /**
     * 대분류별 소분류 매핑 (표시명 List로 반환)
     * 
     * @param majorCategory 대분류
     * @return 해당 대분류에 속하는 소분류 표시명 List
     */
    public static List<String> getMinorCategoryDisplayNamesByMajor(MajorCategory majorCategory) {
        return getMinorCategoriesListByMajor(majorCategory)
                .stream()
                .map(MinorCategory::getDisplayName)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 대분류 반환
     * 
     * @return 모든 대분류 배열
     */
    public static MajorCategory[] getAllMajorCategories() {
        return MajorCategory.values();
    }
    
    /**
     * 모든 대분류 표시명 반환
     * 
     * @return 모든 대분류 표시명 List
     */
    public static List<String> getAllMajorCategoryDisplayNames() {
        return Arrays.stream(MajorCategory.values())
                .map(MajorCategory::getDisplayName)
                .collect(Collectors.toList());
    }
    
    /**
     * 대분류가 유효한지 확인
     * 
     * @param majorCategory 확인할 대분류
     * @return 유효한 대분류인지 여부
     */
    public static boolean isValidMajorCategory(MajorCategory majorCategory) {
        return majorCategory != null;
    }
    
    /**
     * 소분류가 유효한지 확인
     * 
     * @param minorCategory 확인할 소분류
     * @return 유효한 소분류인지 여부
     */
    public static boolean isValidMinorCategory(MinorCategory minorCategory) {
        return minorCategory != null;
    }
}
