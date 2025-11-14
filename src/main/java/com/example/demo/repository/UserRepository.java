package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 데이터를 관리하는 JPA 리포지토리
 * Spring Data JPA의 JpaRepository를 상속받아 기본적인 CRUD 작업을 자동으로 제공
 * 
 * 주요 기능:
 * - 사용자 생성, 조회, 수정, 삭제
 * - 제공자 ID로 사용자 검색
 * - 사용자 존재 여부 확인
 */
@Repository // Spring이 이 클래스를 데이터 접근 계층의 컴포넌트로 인식
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 제공자 ID로 사용자를 검색
     * 
     * @param providerId 제공자에서 제공하는 사용자 ID
     * @return 사용자 정보를 담은 Optional (사용자가 없으면 빈 Optional)
     */
    Optional<User> findByProviderId(String providerId);
    
    /**
     * 특정 제공자 ID를 가진 사용자가 존재하는지 확인
     * 
     * @param providerId 제공자에서 제공하는 사용자 ID
     * @return 사용자가 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByProviderId(String providerId);
    
    /**
     * UUID로 사용자를 검색 (외부 노출용)
     * 
     * @param userId 사용자 UUID
     * @return 사용자 정보를 담은 Optional (사용자가 없으면 빈 Optional)
     */
    Optional<User> findByUserId(String userId);
    
    /**
     * 특정 UUID를 가진 사용자가 존재하는지 확인
     * 
     * @param userId 사용자 UUID
     * @return 사용자가 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByUserId(String userId);
}
