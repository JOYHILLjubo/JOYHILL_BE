package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Team;
import com.joyhill.demo.domain.TeamRole;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.TeamRepository;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final UserRepository userRepository;
    private final AccessGuard accessGuard;

    public TeamService(TeamRepository teamRepository, TeamRoleRepository teamRoleRepository,
                       UserRepository userRepository, AccessGuard accessGuard) {
        this.teamRepository = teamRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
    }

    // ── 팀 목록 ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return teamRepository.findAll().stream().map(team -> {
            Map<String, Object> map = new HashMap<>();
            map.put("teamName", team.getTeamName());
            map.put("intro", team.getIntro());
            long memberCount = teamRoleRepository.findByTeamName(team.getTeamName()).size();
            map.put("memberCount", memberCount);
            return map;
        }).toList();
    }

    // ── 팀원 목록 ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> members(String teamName) {
        getTeam(teamName); // 팀 존재 여부 확인
        return teamRoleRepository.findByTeamName(teamName).stream().map(tr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", tr.getUserId());
            map.put("isLeader", tr.isLeader());
            userRepository.findById(tr.getUserId()).ifPresent(user -> {
                map.put("name", user.getName());
                map.put("phone", user.getPhone());
                map.put("birth", user.getBirth());
                map.put("famName", user.getFamName());
                map.put("role", user.getRole());
            });
            return map;
        }).sorted(Comparator.comparing(
                (Map<String, Object> m) -> m.get("isLeader") == Boolean.TRUE ? 0 : 1
        ).thenComparing(
                m -> String.valueOf(m.getOrDefault("name", "")),
                Collator.getInstance(Locale.KOREAN)
        )).toList();
    }

    // ── 팀원 추가 ──
    public Map<String, Object> addMember(AuthUser authUser, String teamName, AuthDtos.TeamMemberAddRequest request) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        getTeam(teamName);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 이미 소속된 경우 리더 여부만 업데이트
        teamRoleRepository.findByUserIdAndTeamName(request.userId(), teamName).ifPresentOrElse(
                tr -> {
                    // 기존 엔티티를 직접 수정할 수 없으므로 삭제 후 재생성
                    teamRoleRepository.deleteByUserIdAndTeamName(request.userId(), teamName);
                    teamRoleRepository.save(new TeamRole(request.userId(), teamName, request.leader()));
                },
                () -> teamRoleRepository.save(new TeamRole(request.userId(), teamName, request.leader()))
        );

        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("name", user.getName());
        map.put("isLeader", request.leader());
        return map;
    }

    // ── 팀장 위임 ──
    public void delegateLeader(AuthUser authUser, String teamName, Long targetUserId) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        getTeam(teamName);

        // 대상이 팀원인지 확인
        teamRoleRepository.findByUserIdAndTeamName(targetUserId, teamName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "해당 팀원을 찾을 수 없습니다."));

        // 기존 팀장 전원 팀원으로 강등
        List<TeamRole> currentLeaders = teamRoleRepository.findByTeamName(teamName)
                .stream().filter(TeamRole::isLeader).toList();
        for (TeamRole leader : currentLeaders) {
            teamRoleRepository.deleteByUserIdAndTeamName(leader.getUserId(), teamName);
            teamRoleRepository.save(new TeamRole(leader.getUserId(), teamName, false));
        }

        // 대상을 팀장으로 승격
        teamRoleRepository.deleteByUserIdAndTeamName(targetUserId, teamName);
        teamRoleRepository.save(new TeamRole(targetUserId, teamName, true));
    }

    // ── 팀원 제거 ──
    public void removeMember(AuthUser authUser, String teamName, Long userId) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        getTeam(teamName);
        teamRoleRepository.deleteByUserIdAndTeamName(userId, teamName);
    }

    // ── 팀 소개 수정 ──
    public Map<String, Object> updateIntro(AuthUser authUser, String teamName, AuthDtos.TeamIntroRequest request) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        Team team = getTeam(teamName);
        team.setIntro(request.intro());
        return Map.of("teamName", team.getTeamName(), "intro", team.getIntro() == null ? "" : team.getIntro());
    }

    private Team getTeam(String teamName) {
        return teamRepository.findByTeamName(teamName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팀을 찾을 수 없습니다: " + teamName));
    }
}
