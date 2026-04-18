package com.joyhill.demo.repository;
import com.joyhill.demo.domain.SubTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubTeamMemberRepository extends JpaRepository<SubTeamMember, Long> {
    List<SubTeamMember> findBySubTeamId(Long subTeamId);
    Optional<SubTeamMember> findBySubTeamIdAndUserId(Long subTeamId, Long userId);
    boolean existsBySubTeamIdAndUserId(Long subTeamId, Long userId);
    void deleteBySubTeamIdAndUserId(Long subTeamId, Long userId);
    void deleteBySubTeamId(Long subTeamId);
}
