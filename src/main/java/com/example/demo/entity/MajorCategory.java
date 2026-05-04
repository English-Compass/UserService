package com.example.demo.entity;

/**
 * 대분류 카테고리 enum
 * 사용자가 선택할 수 있는 주요 카테고리들을 정의
 */
public enum MajorCategory {
    
    STUDY("학습"),
    BUSINESS("업무"),
    TRAVEL("여행"),
    DAILY_LIFE("일상생활");
    
    private final String displayName;
    
    MajorCategory(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * 표시용 이름 반환
     * @return 한글 표시명
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 문자열로부터 MajorCategory 찾기
     * @param name 카테고리 이름
     * @return 해당하는 MajorCategory, 없으면 null
     */
    public static MajorCategory fromString(String name) {
        try {
            return MajorCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
