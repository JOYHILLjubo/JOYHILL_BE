package com.joyhill.demo.repository;

import com.joyhill.demo.domain.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRoleRepository extends JpaRepository<TeamRole, Long> {
    List<TeamRole> findByUserId(Long userId);
    List<TeamRole> findByTeamName(String teamName);
    Optional<TeamRole> findByUserIdAndTeamName(Long userId, String teamName);
    boolean existsByUserIdAndLeaderTrue(Long userId);
    boolean existsByUserIdAndTeamNameAndLeaderTrue(Long userId, String teamName);
    boolean existsByTeamName(String teamName);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndTeamName(Long userId, String teamName);
}
