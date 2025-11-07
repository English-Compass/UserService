package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 사용자 선호도 변경 이벤트 (카프카 메시지)
 * ProblemService에서 구독하여 사용자 정보를 비동기적으로 처리
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceEvent {
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 사용자 선택 카테고리 (대분류: [소분류들] 형태)
     * 예: {"STUDY": ["CLASS_LISTENING", "DEPARTMENT_CONVERSATION"], "BUSINESS": ["MEETING_CONFERENCE"]}
     */
    private Map<String, List<String>> categories;
    
    /**
     * 사용자 난이도 레벨 (1: 초급, 2: 중급, 3: 고급)
     */
    private Integer difficulty;
    
    /**
     * 업데이트 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime updatedAt;
    
    /**
     * 이벤트 타입 (DIFFICULTY, CATEGORIES, BOTH)
     */
    private String eventType;
}

