package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserCategory;
import com.example.demo.repository.UserCategoryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 카테고리 관리를 위한 서비스 클래스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCategoryService {
    
    private final UserCategoryRepository userCategoryRepository;
    private final UserRepository userRepository;
    private final UserCacheService userCacheService;
    private final EntityManager entityManager;
    private final KafkaProducerService kafkaProducerService;
    
    /**
     * 사용자가 선택한 카테고리들을 저장
     * 
     * @param userId 사용자 ID
     * @param categories 카테고리 정보 (대분류: [소분류들] 형태)
     * @return 저장된 카테고리 목록
     */
    @Transactional
    public List<UserCategory> saveUserCategories(Long userId, Map<String, List<String>> categories) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // 기존 카테고리 삭제 (플러시를 통해 즉시 반영)
        List<UserCategory> existingCategories = userCategoryRepository.findByUser_UserId(userId);
        if (!existingCategories.isEmpty()) {
            userCategoryRepository.deleteAll(existingCategories);
            // 삭제를 즉시 DB에 반영 (플러시) - 중복 삽입 방지
            entityManager.flush();
            log.info("Deleted {} existing categories for user: {}", existingCategories.size(), userId);
        }
        
        // 새로운 카테고리들 저장 (요청 내 중복 제거)
        Set<String> seenCategoryKeys = new HashSet<>(); // 중복 체크를 위한 Set
        List<UserCategory> userCategories = categories.entrySet().stream()
                .flatMap(entry -> {
                    String majorCategory = entry.getKey();
                    // 소분류 리스트에서 중복 제거
                    Set<String> uniqueMinorCategories = new LinkedHashSet<>(entry.getValue());
                    
                    return uniqueMinorCategories.stream()
                            .map(minorCategory -> {
                                // (major, minor) 조합의 고유 키 생성
                                String categoryKey = majorCategory + ":" + minorCategory;
                                
                                // 중복 체크: 같은 요청 내에서 중복된 (major, minor) 조합 제거
                                if (seenCategoryKeys.contains(categoryKey)) {
                                    log.warn("Duplicate category detected and removed: major={}, minor={}", 
                                            majorCategory, minorCategory);
                                    return null; // 중복이면 null 반환
                                }
                                
                                seenCategoryKeys.add(categoryKey);
                                return UserCategory.builder()
                                        .user(user)
                                        .majorCategory(majorCategory)
                                        .minorCategory(minorCategory)
                                        .build();
                            })
                            .filter(Objects::nonNull); // null 제거
                })
                .collect(Collectors.toList());
        
        log.info("Processing {} unique categories (after deduplication) for user: {}", 
                userCategories.size(), userId);
        
        List<UserCategory> savedCategories = userCategoryRepository.saveAll(userCategories);
        log.info("Saved {} categories for user: {}", savedCategories.size(), userId);
        
        // 캐시 업데이트
        userCacheService.cacheUserCategories(userId, categories);
        
        // 카프카 이벤트 발행 (UserService 내부에서 난이도 정보 조회하여 함께 발행)
        // UserService와 UserCategoryService는 같은 애플리케이션 내의 컴포넌트이므로 직접 조회 가능
        Integer currentDifficulty = userCacheService.getUserDifficulty(userId);
        // 캐시에 없으면 기본값 2 사용 (중급)
        if (currentDifficulty == null) {
            currentDifficulty = 2;
        }
        kafkaProducerService.publishCategoryChangeEvent(userId, categories, currentDifficulty);
        
        return savedCategories;
    }
    
    /**
     * 사용자가 선택한 카테고리들을 조회
     * 캐시 우선 조회: Redis 캐시에서 먼저 조회하고, 없으면 DB에서 조회 후 캐시에 저장
     * 
     * @param userId 사용자 ID
     * @return 대분류별로 그룹화된 카테고리 정보
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getUserCategories(Long userId) {
        // 1. 캐시에서 먼저 조회
        Map<String, List<String>> cachedCategories = userCacheService.getUserCategories(userId);
        if (cachedCategories != null) {
            log.info("✅ [CACHE] Returning categories from cache: userId={}", userId);
            return cachedCategories;
        }
        
        // 2. 캐시 미스 시 DB 조회
        log.info("❌ [DB] Cache miss - fetching categories from database: userId={}", userId);
        List<UserCategory> categories = userCategoryRepository.findByUser_UserId(userId);
        
        Map<String, List<String>> result = categories.stream()
                .collect(Collectors.groupingBy(
                        UserCategory::getMajorCategory,
                        Collectors.mapping(UserCategory::getMinorCategory, Collectors.toList())
                ));
        
        // 3. DB 조회 결과를 캐시에 저장
        userCacheService.cacheUserCategories(userId, result);
        
        return result;
    }
    
    /**
     * 특정 대분류를 선택한 사용자들의 카테고리 조회
     * 
     * @param majorCategory 대분류명
     * @return 해당 대분류를 선택한 사용자들의 카테고리 목록 (예: 기술, 디자인, 마케팅)
     */
    @Transactional(readOnly = true)
    public List<UserCategory> getCategoriesByMajorCategory(String majorCategory) {
        return userCategoryRepository.findByMajorCategory(majorCategory);
    }
    
    /**
     * 특정 소분류를 선택한 사용자들의 카테고리 조회
     * 
     * @param minorCategory 소분류명 (예: 프로그래밍, UI/UX, 디지털마케팅)
     * @return 해당 소분류를 선택한 사용자들의 카테고리 목록
     */
    @Transactional(readOnly = true)
    public List<UserCategory> getCategoriesByMinorCategory(String minorCategory) {
        return userCategoryRepository.findByMinorCategory(minorCategory);
    }
    
    /**
     * 사용자가 특정 대분류를 선택했는지 확인
     * 
     * @param userId 사용자 ID
     * @param majorCategory 대분류명
     * @return 선택했으면 true, 선택하지 않았으면 false
     */
    @Transactional(readOnly = true)
    public boolean hasMajorCategory(Long userId, String majorCategory) {
        return userCategoryRepository.existsByUser_UserIdAndMajorCategory(userId, majorCategory);
    }
    
    /**
     * 사용자가 특정 소분류를 선택했는지 확인
     * 
     * @param userId 사용자 ID
     * @param minorCategory 소분류명
     * @return 선택했으면 true, 선택하지 않았으면 false
     */
    @Transactional(readOnly = true)
    public boolean hasMinorCategory(Long userId, String minorCategory) {
        return userCategoryRepository.existsByUser_UserIdAndMinorCategory(userId, minorCategory);
    }
}
