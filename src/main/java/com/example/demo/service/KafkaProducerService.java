package com.example.demo.service;

import com.example.demo.dto.UserPreferenceEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 카프카 이벤트 발행 서비스
 * 사용자 선호도 변경 시 ProblemService로 이벤트를 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.kafka.topic.user-preference:user-preference-events}")
    private String userPreferenceTopic;
    
    /**
     * 사용자 선호도 변경 이벤트 발행
     * 
     * @param userId 사용자 ID
     * @param categories 카테고리 정보 (null 가능)
     * @param difficulty 난이도 정보 (null 가능)
     * @param eventType 이벤트 타입 (DIFFICULTY, CATEGORIES, BOTH)
     */
    public void publishUserPreferenceEvent(Long userId, 
                                          Map<String, List<String>> categories, 
                                          Integer difficulty, 
                                          String eventType) {
        try {
            UserPreferenceEvent event = UserPreferenceEvent.builder()
                    .userId(userId)
                    .categories(categories)
                    .difficulty(difficulty)
                    .updatedAt(LocalDateTime.now())
                    .eventType(eventType)
                    .build();
            
            String message = objectMapper.writeValueAsString(event);
            
            // userId를 키로 사용하여 같은 사용자의 이벤트가 순서대로 처리되도록 보장
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(userPreferenceTopic, userId.toString(), message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ [KAFKA] 사용자 선호도 이벤트 발행 성공: userId={}, eventType={}, offset={}", 
                            userId, eventType, result.getRecordMetadata().offset());
                } else {
                    log.error("❌ [KAFKA] 사용자 선호도 이벤트 발행 실패: userId={}, eventType={}, error={}", 
                            userId, eventType, ex.getMessage(), ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("❌ [KAFKA] 사용자 선호도 이벤트 직렬화 실패: userId={}, eventType={}, error={}", 
                    userId, eventType, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [KAFKA] 사용자 선호도 이벤트 발행 중 예상치 못한 오류 발생: userId={}, eventType={}, error={}", 
                    userId, eventType, e.getMessage(), e);
        }
    }
    
    /**
     * 난이도 변경 이벤트 발행
     * 
     * @param userId 사용자 ID
     * @param difficulty 난이도 레벨
     * @param categories 현재 카테고리 정보 (캐시 또는 DB에서 조회 필요)
     */
    public void publishDifficultyChangeEvent(Long userId, Integer difficulty, Map<String, List<String>> categories) {
        publishUserPreferenceEvent(userId, categories, difficulty, "DIFFICULTY");
    }
    
    /**
     * 카테고리 변경 이벤트 발행
     * 
     * @param userId 사용자 ID
     * @param categories 카테고리 정보
     * @param difficulty 현재 난이도 정보 (캐시 또는 DB에서 조회 필요)
     */
    public void publishCategoryChangeEvent(Long userId, Map<String, List<String>> categories, Integer difficulty) {
        publishUserPreferenceEvent(userId, categories, difficulty, "CATEGORIES");
    }
}

