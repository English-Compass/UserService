package com.example.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 정보 캐싱을 위한 서비스 클래스
 * 
 * 주요 기능:
 * - 사용자 난이도 레벨 캐싱
 * - 사용자 카테고리 정보 캐싱
 * - 캐시 조회 및 무효화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ObjectMapper는 Spring Boot가 자동으로 빈으로 제공하므로 주입받음
    private final ObjectMapper objectMapper;
    
    private static final String USER_DIFFICULTY_KEY = "user:difficulty:%s";
    private static final String USER_CATEGORIES_KEY = "user:categories:%s";
    private static final long CACHE_TTL_HOURS = 24; // 24시간 캐시 유지
    
    /**
     * 사용자 난이도 레벨 캐싱
     * 
     * @param userId 사용자 ID
     * @param difficultyLevel 난이도 레벨
     */
    public void cacheUserDifficulty(String userId, Integer difficultyLevel) {
        try {
            String key = String.format(USER_DIFFICULTY_KEY, userId);
            redisTemplate.opsForValue().set(key, difficultyLevel, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("✅ [CACHE SET] User difficulty cached: userId={}, level={}, key={}", userId, difficultyLevel, key);
        } catch (Exception e) {
            log.error("❌ [CACHE ERROR] Failed to cache user difficulty: userId={}", userId, e);
        }
    }
    
    /**
     * 사용자 난이도 레벨 조회 (캐시에서)
     * 
     * @param userId 사용자 ID
     * @return 캐시된 난이도 레벨 (없으면 null)
     */
    public Integer getUserDifficulty(String userId) {
        try {
            String key = String.format(USER_DIFFICULTY_KEY, userId);
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.info("✅ [CACHE HIT] User difficulty found in cache: userId={}, level={}, key={}", userId, value, key);
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
            log.info("❌ [CACHE MISS] User difficulty not in cache: userId={}, key={}", userId, key);
            return null;
        } catch (Exception e) {
            log.error("❌ [CACHE ERROR] Failed to get user difficulty from cache: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 사용자 카테고리 캐싱
     * 
     * @param userId 사용자 ID
     * @param categories 카테고리 정보
     */
    public void cacheUserCategories(String userId, Map<String, List<String>> categories) {
        try {
            String key = String.format(USER_CATEGORIES_KEY, userId);
            redisTemplate.opsForValue().set(key, categories, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("✅ [CACHE SET] User categories cached: userId={}, count={}, key={}", 
                    userId, categories != null ? categories.size() : 0, key);
        } catch (Exception e) {
            log.error("❌ [CACHE ERROR] Failed to cache user categories: userId={}", userId, e);
        }
    }
    
    /**
     * 사용자 카테고리 조회 (캐시에서)
     * 
     * @param userId 사용자 ID
     * @return 캐시된 카테고리 정보 (없으면 null)
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getUserCategories(String userId) {
        try {
            String key = String.format(USER_CATEGORIES_KEY, userId);
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.info("✅ [CACHE HIT] User categories found in cache: userId={}, key={}", userId, key);
                // Redis에서 가져온 Map을 올바른 타입으로 변환
                if (value instanceof Map) {
                    return objectMapper.convertValue(value, 
                        new TypeReference<Map<String, List<String>>>() {});
                }
            }
            log.info("❌ [CACHE MISS] User categories not in cache: userId={}, key={}", userId, key);
            return null;
        } catch (Exception e) {
            log.error("❌ [CACHE ERROR] Failed to get user categories from cache: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 사용자 정보 캐시 무효화 (설정 변경 시)
     * 
     * @param userId 사용자 ID
     */
    public void invalidateUserCache(String userId) {
        try {
            String difficultyKey = String.format(USER_DIFFICULTY_KEY, userId);
            String categoriesKey = String.format(USER_CATEGORIES_KEY, userId);
            redisTemplate.delete(difficultyKey);
            redisTemplate.delete(categoriesKey);
            log.info("Invalidated user cache: userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate user cache: userId={}", userId, e);
        }
    }
    
    /**
     * 사용자 정보 전체 캐싱 (한 번에)
     * 
     * @param userId 사용자 ID
     * @param difficultyLevel 난이도 레벨
     * @param categories 카테고리 정보
     */
    public void cacheUserInfo(String userId, Integer difficultyLevel, Map<String, List<String>> categories) {
        cacheUserDifficulty(userId, difficultyLevel);
        cacheUserCategories(userId, categories);
    }
}

