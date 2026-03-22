package com.joyhill.demo.repository;

import com.joyhill.demo.domain.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRoleRepository extends JpaRepository<TeamRole, Long> {
    List<TeamRole> findByUserId(Long userId);
    boolean existsByUserIdAndLeaderTrue(Long userId);
    // 새가족팀장 여부 체크용
    boolean existsByUserIdAndTeamNameAndLeaderTrue(Long userId, String teamName);
    void deleteByUserId(Long userId);
}
