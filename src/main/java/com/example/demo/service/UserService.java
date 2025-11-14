package com.example.demo.service;

import com.example.demo.dto.UserSettingsResponseDto;
import com.example.demo.entity.User;
import com.example.demo.entity.UserCategory;
import com.example.demo.repository.UserCategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * Spring Security의 UserDetailsService를 구현하여 인증에 필요한 사용자 정보를 제공
 * 
 * 주요 기능:
 * - 사용자 인증 정보 로드
 * - 사용자 생성 및 수정
 * - OAuth2 로그인 시 사용자 정보 관리
 * - 사용자 검색 및 존재 여부 확인
 * - 사용자 난이도 설정 및 조회
 */
@Service // Spring이 이 클래스를 서비스 계층의 컴포넌트로 인식
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
@Slf4j // Lombok: 로깅을 위한 log 필드 자동 생성
@Transactional // 클래스 레벨에서 트랜잭션 관리
public class UserService implements UserDetailsService {
    
    /**
     * 사용자 데이터 접근을 위한 리포지토리
     * 생성자 주입으로 의존성 주입
     */
    private final UserRepository userRepository;
    
    /**
     * 사용자 정보 캐싱을 위한 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final UserCacheService userCacheService;
    
    /**
     * 카프카 이벤트 발행을 위한 서비스
     * 생성자 주입으로 의존성 주입
     */
    private final KafkaProducerService kafkaProducerService;
    
    /**
     * 사용자 카테고리 리포지토리
     * UserService 애플리케이션 내에서 공유하는 리포지토리
     * (UserService와 UserCategoryService는 같은 애플리케이션 내의 컴포넌트이므로 직접 조회 가능)
     * 이벤트 발행 시 카테고리 정보를 함께 포함하기 위해 사용
     */
    private final UserCategoryRepository userCategoryRepository;
    
    
    /**
     * Spring Security에서 사용자 인증 시 호출되는 메서드
     * providerId를 사용하여 사용자 정보를 로드
     * 
     * @param providerId 카카오에서 제공하는 사용자 ID
     * @return Spring Security에서 사용할 수 있는 UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 성능 최적화
    public UserDetails loadUserByUsername(String providerId) throws UsernameNotFoundException {
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with providerId: " + providerId));
        return new CustomUserDetails(user);
    }
    
    /**
     * 제공자 ID로 사용자를 검색
     * OAuth2 로그인 시 사용자를 식별하는 데 사용
     * 
     * @param providerId 제공자에서 제공하는 사용자 ID
     * @return 사용자 정보를 담은 Optional (사용자가 없으면 빈 Optional)
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 성능 최적화
    public Optional<User> findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }
    
    /**
     * 사용자 ID로 사용자를 검색 (UUID 문자열)
     * 
     * @param userId 사용자 ID (UUID 문자열)
     * @return 사용자 정보를 담은 Optional (사용자가 없으면 빈 Optional)
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 성능 최적화
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }
    
    /**
     * 사용자 ID로 사용자를 검색 (Long 타입 - 내부 ID)
     * 
     * @param userId 사용자 ID (Long 타입)
     * @return 사용자 정보를 담은 Optional (사용자가 없으면 빈 Optional)
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 성능 최적화
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * 사용자 정보를 데이터베이스에 저장
     * 
     * @param user 저장할 사용자 객체
     * @return 저장된 사용자 객체 (ID가 설정됨)
     */
    public User saveUser(User user) {
        log.info("Saving user: {}", user.getName()); // 로그 기록
        return userRepository.save(user);
    }
    
    /**
     * OAuth2 로그인 시 사용자 정보를 생성하거나 업데이트
     * 기존 사용자가 있으면 정보를 업데이트하고, 없으면 새로 생성
     * 
     * @param name 사용자 이름 (닉네임)
     * @param profileImage 사용자 프로필 이미지 URL
     * @param providerId 카카오에서 제공하는 사용자 ID
     * @return 생성되거나 업데이트된 사용자 객체
     */
    public User createOrUpdateUser(String name, String profileImage, String providerId) {
        log.info("=== createOrUpdateUser 호출됨 ===");
        log.info("name: {}", name);
        log.info("profileImage: {}", profileImage);
        log.info("providerId: {}", providerId);
        
        try {
            // 기존 사용자가 있는지 확인
            log.info("기존 사용자 검색 시작...");
            Optional<User> existingUser = findByProviderId(providerId);
            log.info("기존 사용자 검색 결과: {}", existingUser.isPresent() ? "존재함" : "존재하지 않음");
            
            if (existingUser.isPresent()) {
                // 기존 사용자 정보 업데이트
                User user = existingUser.get();
                log.info("기존 사용자 정보: userId={}, name={}", user.getUserId(), user.getName());
                
                user.setName(name);
                user.setProfileImage(profileImage);
                log.info("사용자 정보 업데이트 중...");
                User updatedUser = userRepository.save(user);
                log.info("사용자 정보 업데이트 완료: userId={}", updatedUser.getUserId());
                return updatedUser;
            } else {
                // 새 사용자 생성
                log.info("새 사용자 생성 시작...");
                User newUser = User.builder()
                        .name(name)
                        .profileImage(profileImage)
                        .providerId(providerId)
                        .build();
                
                log.info("새 사용자 객체 생성됨: {}", newUser);
                User savedUser = userRepository.save(newUser);
                log.info("새 사용자 저장 완료: userId={}", savedUser.getUserId());
                return savedUser;
            }
        } catch (Exception e) {
            log.error("createOrUpdateUser 실행 중 에러 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 사용자 난이도 설정 (1: 초급, 2: 중급, 3: 고급)
     * 
     * @param userId 사용자 ID
     * @param difficultyLevel 난이도 레벨 (1, 2, 3)
     * @return 업데이트된 사용자 객체
     */
    @Transactional
    public User setUserDifficultyLevel(String userId, Integer difficultyLevel) {
        log.info("사용자 난이도 설정: userId={}, level={}", userId, difficultyLevel);
        
        // 난이도 유효성 검사
        if (difficultyLevel == null || difficultyLevel < 1 || difficultyLevel > 3) {
            throw new IllegalArgumentException("Invalid difficulty level. Must be 1, 2, or 3");
        }
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + userId));
        
        user.setDifficultyLevel(difficultyLevel);
        User updatedUser = userRepository.save(user);
        
        // 캐시 업데이트
        userCacheService.cacheUserDifficulty(userId, difficultyLevel);
        
        // 카프카 이벤트 발행 (UserService 내부에서 카테고리 정보 조회하여 함께 발행)
        // UserService와 UserCategoryService는 같은 애플리케이션 내의 컴포넌트이므로 직접 조회 가능
        Map<String, List<String>> currentCategories = getUserCategoriesFromCacheOrRepository(userId);
        kafkaProducerService.publishDifficultyChangeEvent(userId, difficultyLevel, currentCategories);
        
        log.info("사용자 난이도 설정 완료: userId={}, level={}", userId, difficultyLevel);
        return updatedUser;
    }
    
    /**
     * 사용자 난이도 조회
     * 캐시 우선 조회: Redis 캐시에서 먼저 조회하고, 없으면 DB에서 조회 후 캐시에 저장
     * 
     * @param userId 사용자 ID
     * @return 난이도 레벨 (1, 2, 3)
     */
    @Transactional(readOnly = true)
    public Integer getUserDifficultyLevel(String userId) {
        log.info("사용자 난이도 조회: userId={}", userId);
        
        // 1. 캐시에서 먼저 조회
        Integer cachedDifficulty = userCacheService.getUserDifficulty(userId);
        if (cachedDifficulty != null) {
            log.info("✅ [CACHE] Returning difficulty from cache: userId={}, level={}", userId, cachedDifficulty);
            return cachedDifficulty;
        }
        
        // 2. 캐시 미스 시 DB 조회
        log.info("❌ [DB] Cache miss - fetching difficulty from database: userId={}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + userId));
        
        Integer difficultyLevel = user.getDifficultyLevel();
        
        // 3. DB 조회 결과를 캐시에 저장
        if (difficultyLevel != null) {
            userCacheService.cacheUserDifficulty(userId, difficultyLevel);
        }
        
        log.info("사용자 난이도 조회 완료: userId={}, level={}", userId, difficultyLevel);
        return difficultyLevel;
    }
    
    /**
     * 사용자 난이도 초기화 (기본값: 2)
     * 
     * @param userId 사용자 ID
     * @return 업데이트된 사용자 객체
     */
    @Transactional
    public User resetUserDifficultyLevel(String userId) {
        log.info("사용자 난이도 초기화: userId={}", userId);
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + userId));
        
        user.setDifficultyLevel(2); // 기본값: 중급
        User updatedUser = userRepository.save(user);
        
        // 캐시 업데이트
        userCacheService.cacheUserDifficulty(userId, 2);
        
        // 카프카 이벤트 발행 (UserService 내부에서 카테고리 정보 조회하여 함께 발행)
        // UserService와 UserCategoryService는 같은 애플리케이션 내의 컴포넌트이므로 직접 조회 가능
        Map<String, List<String>> currentCategories = getUserCategoriesFromCacheOrRepository(userId);
        kafkaProducerService.publishDifficultyChangeEvent(userId, 2, currentCategories);
        
        log.info("사용자 난이도 초기화 완료: userId={}, level=2", userId);
        return updatedUser;
    }
    
    /**
     * 사용자 설정 정보 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 설정 정보 응답 DTO
     */
    @Transactional(readOnly = true)
    public UserSettingsResponseDto getUserSettings(String userId) {
        log.info("사용자 설정 정보 조회: userId={}", userId);
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + userId));
        
        UserSettingsResponseDto response = UserSettingsResponseDto.success(
                user.getUserId(),
                user.getName(),
                user.getProfileImage(),
                user.getDifficultyLevel(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        log.info("사용자 설정 정보 조회 완료: userId={}, name={}, difficultyLevel={}", 
                userId, user.getName(), user.getDifficultyLevel());
        
        return response;
    }
    
    /**
     * 사용자 카테고리 조회 (캐시 우선, 없으면 DB 조회)
     * UserService 애플리케이션 내에서 공유하는 리포지토리를 사용
     * (UserService와 UserCategoryService는 같은 애플리케이션 내의 컴포넌트이므로 직접 조회 가능)
     * 
     * @param userId 사용자 ID
     * @return 대분류별로 그룹화된 카테고리 정보
     */
    @Transactional(readOnly = true)
    private Map<String, List<String>> getUserCategoriesFromCacheOrRepository(String userId) {
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
}
