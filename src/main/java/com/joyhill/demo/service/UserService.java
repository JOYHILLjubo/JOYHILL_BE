package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.domain.TeamRole;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class UserService {

    private final UserRepository userRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final AccessGuard accessGuard;
    private final OrganizationService organizationService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TeamRoleRepository teamRoleRepository,
                       AccessGuard accessGuard,
                       OrganizationService organizationService, AuthService authService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.accessGuard = accessGuard;
        this.organizationService = organizationService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me(AuthUser authUser) {
        User user = getUser(authUser.userId());
        return authService.toSummary(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> users(AuthUser authUser, Role role, String search) {
        // 사역팀 팀장 또는 leader 이상 허용 (팀원 추가 기능 지원)
        boolean isTeamLeader = teamRoleRepository.existsByUserIdAndLeaderTrue(authUser.userId());
        if (!authUser.role().atLeast(Role.leader) && !isTeamLeader) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
        String keyword = search == null ? "" : search;
        List<User> users;
        if (role != null) {
            users = userRepository.findByRoleAndNameContainingIgnoreCase(role, keyword);
        } else {
            users = userRepository.findByNameContainingIgnoreCase(keyword);
        }
        return users.stream()
                .sorted(Comparator.comparing(User::getName, Comparator.nullsLast(Collator.getInstance(Locale.KOREAN)::compare)))
                .map(this::toMap).toList();
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.UserCreateRequest request) {
        accessGuard.requireAdmin(authUser);
        String normalizedPhone = PhoneUtils.normalize(request.phone());
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new ApiException(ErrorCode.DUPLICATE_PHONE, "이미 등록된 전화번호입니다.");
        }
        User user = new User();
        user.setName(request.name());
        user.setPhone(normalizedPhone);
        user.setBirth(request.birth());
        user.setRole(request.role() != null ? request.role() : Role.member);
        user.setFamName(request.famName());
        user.setVillageName(request.villageName());
        userRepository.save(user);

        // 사역팀 추가
        if (request.teams() != null) {
            for (String teamName : request.teams()) {
                boolean isLeader = request.teamRoles() != null && request.teamRoles().contains(teamName);
                teamRoleRepository.save(new TeamRole(user.getId(), teamName, isLeader));
            }
        }

        return toMap(user);
    }

    public Map<String, Object> update(AuthUser authUser, Long id, AuthDtos.UserUpdateRequest request) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        if (request.name() != null) user.setName(request.name());
        if (request.phone() != null) user.setPhone(PhoneUtils.normalize(request.phone()));
        if (request.birth() != null) user.setBirth(request.birth());
        if (request.famName() != null) user.setFamName(request.famName());
        if (request.villageName() != null) user.setVillageName(request.villageName());
        return toMap(user);
    }

    public void changeRole(AuthUser authUser, Long id, AuthDtos.RoleUpdateRequest request) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        user.setRole(request.role());
    }

    public void delete(AuthUser authUser, Long id) {
        accessGuard.requireAdmin(authUser);
        teamRoleRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    public void resetPassword(AuthUser authUser, Long id) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        user.setPassword(null);
        user.setPasswordChanged(false);
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("phone", PhoneUtils.format(user.getPhone()));
        map.put("birth", user.getBirth());
        map.put("role", user.getRole());
        map.put("famName", user.getFamName());
        map.put("villageName", user.getVillageName());
        var teamRoles = teamRoleRepository.findByUserId(user.getId());
        map.put("teams", teamRoles.stream().map(TeamRole::getTeamName).toList());
        map.put("teamRoles", teamRoles.stream().filter(TeamRole::isLeader).map(TeamRole::getTeamName).toList());
        map.put("summary", Map.of(
                "name", user.getName() != null ? user.getName() : "",
                "fam", user.getFamName() != null ? user.getFamName() : "",
                "village", user.getVillageName() != null ? user.getVillageName() : "",
                "phone", PhoneUtils.format(user.getPhone()),
                "role", user.getRole()
        ));
        return map;
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
