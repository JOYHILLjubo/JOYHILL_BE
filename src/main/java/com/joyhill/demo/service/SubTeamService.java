package com.joyhill.demo.service;
import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.SubTeam;
import com.joyhill.demo.domain.SubTeamMember;
import com.joyhill.demo.domain.TeamRole;
import com.joyhill.demo.repository.SubTeamMemberRepository;
import com.joyhill.demo.repository.SubTeamRepository;
import com.joyhill.demo.repository.TeamRepository;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@Transactional
public class SubTeamService {
    private final SubTeamRepository subTeamRepository;
    private final SubTeamMemberRepository subTeamMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final UserRepository userRepository;
    private final AccessGuard accessGuard;
    public SubTeamService(SubTeamRepository subTeamRepository,
                          SubTeamMemberRepository subTeamMemberRepository,
                          TeamRepository teamRepository,
                          TeamRoleRepository teamRoleRepository,
                          UserRepository userRepository,
                          AccessGuard accessGuard) {
        this.subTeamRepository = subTeamRepository;
        this.subTeamMemberRepository = subTeamMemberRepository;
        this.teamRepository = teamRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSubTeams(String teamName) {
        requireTeamExists(teamName);
        return subTeamRepository.findByTeamName(teamName).stream()
                .map(st -> buildSubTeamMap(st, true))
                .toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> members(String teamName, String subTeamName) {
        SubTeam subTeam = getSubTeam(teamName, subTeamName);
        return buildMemberList(subTeam);
    }
    public Map<String, Object> createSubTeam(AuthUser authUser, String teamName,
                                              AuthDtos.SubTeamCreateRequest request) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        requireTeamExists(teamName);
        if (subTeamRepository.existsByTeamNameAndSubTeamName(teamName, request.subTeamName())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "이미 존재하는 서브팀 이름입니다: " + request.subTeamName());
        }
        SubTeam subTeam = subTeamRepository.save(new SubTeam(teamName, request.subTeamName()));
        return buildSubTeamMap(subTeam, false);
    }
    public void deleteSubTeam(AuthUser authUser, String teamName, String subTeamName) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        SubTeam subTeam = getSubTeam(teamName, subTeamName);
        subTeamMemberRepository.deleteBySubTeamId(subTeam.getId());
        subTeamRepository.delete(subTeam);
    }
    public Map<String, Object> addMember(AuthUser authUser, String teamName, String subTeamName,
                                          AuthDtos.SubTeamMemberAddRequest request) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        SubTeam subTeam = getSubTeam(teamName, subTeamName);
        var user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (subTeamMemberRepository.existsBySubTeamIdAndUserId(subTeam.getId(), request.userId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "이미 해당 서브팀에 소속된 팀원입니다.");
        }
        subTeamMemberRepository.save(new SubTeamMember(subTeam.getId(), request.userId()));
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("name", user.getName());
        map.put("subTeamName", subTeamName);
        return map;
    }
    public void removeMember(AuthUser authUser, String teamName, String subTeamName, Long userId) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        SubTeam subTeam = getSubTeam(teamName, subTeamName);
        subTeamMemberRepository.deleteBySubTeamIdAndUserId(subTeam.getId(), userId);
    }
    public Map<String, Object> setLeader(AuthUser authUser, String teamName, String subTeamName,
                                          AuthDtos.SubTeamLeaderRequest request) {
        accessGuard.requireTeamLeaderOrAbove(authUser, teamName);
        SubTeam subTeam = getSubTeam(teamName, subTeamName);
        if (request.userId() != null &&
                !subTeamMemberRepository.existsBySubTeamIdAndUserId(subTeam.getId(), request.userId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "서브팀 멤버만 리더로 지정할 수 있습니다.");
        }
        subTeam.setLeaderUserId(request.userId());
        return buildSubTeamMap(subTeam, false);
    }
    private SubTeam getSubTeam(String teamName, String subTeamName) {
        return subTeamRepository.findByTeamNameAndSubTeamName(teamName, subTeamName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "서브팀을 찾을 수 없습니다: " + subTeamName));
    }
    private void requireTeamExists(String teamName) {
        if (!teamRepository.existsByTeamName(teamName)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "팀을 찾을 수 없습니다: " + teamName);
        }
    }
    private Map<String, Object> buildSubTeamMap(SubTeam subTeam, boolean includeMembers) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", subTeam.getId());
        map.put("teamName", subTeam.getTeamName());
        map.put("subTeamName", subTeam.getSubTeamName());
        map.put("leaderUserId", subTeam.getLeaderUserId());
        if (subTeam.getLeaderUserId() != null) {
            userRepository.findById(subTeam.getLeaderUserId()).ifPresent(u -> map.put("leaderName", u.getName()));
        }
        // 팀 전체 팀장 이름 포함 (서브팀에서도 팀장으로 표시하기 위해)
        teamRoleRepository.findByTeamName(subTeam.getTeamName()).stream()
                .filter(TeamRole::isLeader)
                .findFirst()
                .flatMap(tr -> userRepository.findById(tr.getUserId()))
                .ifPresent(u -> map.put("teamLeaderName", u.getName()));
        if (includeMembers) {
            map.put("members", buildMemberList(subTeam));
        }
        return map;
    }
    private List<Map<String, Object>> buildMemberList(SubTeam subTeam) {
        return subTeamMemberRepository.findBySubTeamId(subTeam.getId()).stream()
                .map(stm -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", stm.getUserId());
                    m.put("isLeader", stm.getUserId().equals(subTeam.getLeaderUserId()));
                    userRepository.findById(stm.getUserId()).ifPresent(u -> {
                        m.put("name", u.getName());
                        m.put("phone", u.getPhone());
                        m.put("birth", u.getBirth());
                        m.put("famName", u.getFamName());
                        m.put("role", u.getRole());
                        m.put("avatarKey", u.getAvatarKey());
                    });
                    return m;
                }).toList();
    }
}
