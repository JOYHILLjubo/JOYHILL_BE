package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Role;
import com.joyhill.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    // 기본 역할 기반 조회
    List<User> findByRole(Role role);
    List<User> findByNameContainingIgnoreCase(String search);
    List<User> findByRoleAndNameContainingIgnoreCase(Role role, String search);

    // 팸 기반 조회 (구 FamMemberRepository 역할 통합)
    List<User> findByFamName(String famName);
    List<User> findByFamNameIn(List<String> famNames);
    long countByFamName(String famName);
    long countByFamNameAndIdNot(String famName, Long id);
    long countByFamNameAndNameNot(String famName, String name);

    // 팸 미배정 유저 (newcomer에서 넘어오기 전 상태 등)
    List<User> findByFamNameIsNull();
}
