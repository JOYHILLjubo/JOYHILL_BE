package com.joyhill.demo.repository;
import com.joyhill.demo.domain.SubTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubTeamRepository extends JpaRepository<SubTeam, Long> {
    List<SubTeam> findByTeamName(String teamName);
    Optional<SubTeam> findByTeamNameAndSubTeamName(String teamName, String subTeamName);
    boolean existsByTeamNameAndSubTeamName(String teamName, String subTeamName);
    void deleteByTeamNameAndSubTeamName(String teamName, String subTeamName);
}
