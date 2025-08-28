package com.example.demo.repository;

import com.example.demo.entity.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * UserCategory 엔티티를 위한 JPA Repository
 */
@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    
    /**
     * 특정 사용자가 선택한 모든 카테고리 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자가 선택한 카테고리 목록
     */
    List<UserCategory> findByUser_UserId(Long userId);
    
    /**
     * 특정 대분류를 선택한 사용자들의 카테고리 조회
     * 
     * @param mainCategory 대분류명
     * @return 해당 대분류를 선택한 사용자들의 카테고리 목록
     */
    List<UserCategory> findByMajorCategory(String mainCategory);
    
    /**
     * 특정 소분류를 선택한 사용자들의 카테고리 조회
     * 
     * @param subCategory 소분류명
     * @return 해당 소분류를 선택한 사용자들의 카테고리 목록
     */
    List<UserCategory> findByMinorCategory(String subCategory);
    
    /**
     * 특정 사용자가 특정 대분류를 선택했는지 확인
     * 
     * @param userId 사용자 ID
     * @param majorCategory 대분류명
     * @return 선택했으면 true, 선택하지 않았으면 false
     */
    boolean existsByUser_UserIdAndMajorCategory(Long userId, String majorCategory);    
    
    /**
     * 특정 사용자가 특정 소분류를 선택했는지 확인
     * 
     * @param userId 사용자 ID
     * @param minorCategory 소분류명
     * @return 선택했으면 true, 선택하지 않았으면 false
     */
    boolean existsByUser_UserIdAndMinorCategory(Long userId, String minorCategory);
    
    /**
     * 사용자 ID와 대분류로 카테고리 조회
     * 
     * @param userId 사용자 ID
     * @param majorCategory 대분류명
     * @return 해당 사용자가 선택한 특정 대분류의 카테고리 목록
     */
    List<UserCategory> findByUser_UserIdAndMajorCategory(Long userId, String majorCategory);
    
}
