package com.example.demo.entity;

/**
 * 소분류 카테고리 enum
 * 각 대분류에 속하는 세부 카테고리들을 정의
 */
public enum MinorCategory {
    
    // 학습 관련
    
    CLASS_LISTENING("수업 듣기"),
    DEPARTMENT_CONVERSATION("학과 대화"),
    ASSIGNMENT_EXAM("과제/시험"),
    
    // 업무 관련
    MEETING_CONFERENCE("회의/컨퍼런스"),
    CUSTOMER_SERVICE("고객 서비스"),
    EMAIL_REPORT("이메일/보고서"),
    
    // 여행 관련
    BACKPACKING("백패킹"),
    FAMILY_TRIP("가족 여행"),
    FRIEND_TRIP("친구 여행"),
    
    // 일상생활 관련
    SHOPPING_DINING("쇼핑/외식"),
    HOSPITAL_VISIT("병원 방문"),
    PUBLIC_TRANSPORT("대중교통");
    
    private final String displayName;
    
    MinorCategory(String displayName) {
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
     * 문자열로부터 MinorCategory 찾기
     * @param name 카테고리 이름
     * @return 해당하는 MinorCategory, 없으면 null
     */
    public static MinorCategory fromString(String name) {
        try {
            return MinorCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
