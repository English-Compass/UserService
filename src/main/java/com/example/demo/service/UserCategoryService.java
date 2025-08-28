package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserCategory;
import com.example.demo.repository.UserCategoryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
        
        // 기존 카테고리 삭제
        List<UserCategory> existingCategories = userCategoryRepository.findByUser_UserId(userId);
        if (!existingCategories.isEmpty()) {
            userCategoryRepository.deleteAll(existingCategories);
        }
        
        // 새로운 카테고리들 저장
        List<UserCategory> userCategories = categories.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(minorCategory -> UserCategory.builder()
                                .user(user)
                                .majorCategory(entry.getKey())
                                .minorCategory(minorCategory)
                                .build()))
                .collect(Collectors.toList());
        
        List<UserCategory> savedCategories = userCategoryRepository.saveAll(userCategories);
        log.info("Saved {} categories for user: {}", savedCategories.size(), userId);
        
        return savedCategories;
    }
    
    /**
     * 사용자가 선택한 카테고리들을 조회
     * 
     * @param userId 사용자 ID
     * @return 대분류별로 그룹화된 카테고리 정보
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getUserCategories(Long userId) {
        List<UserCategory> categories = userCategoryRepository.findByUser_UserId(userId);
        
        return categories.stream()
                .collect(Collectors.groupingBy(
                        UserCategory::getMajorCategory,
                        Collectors.mapping(UserCategory::getMinorCategory, Collectors.toList())
                ));
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
