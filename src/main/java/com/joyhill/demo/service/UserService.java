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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final AccessGuard accessGuard;
    private final OrganizationService organizationService;
    private final AuthService authService;

    public UserService(UserRepository userRepository, TeamRoleRepository teamRoleRepository,
                       AccessGuard accessGuard,
                       OrganizationService organizationService, AuthService authService) {
        this.userRepository = userRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.accessGuard = accessGuard;
        this.organizationService = organizationService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me(AuthUser authUser) {
        User user = getUser(authUser.userId());
        return authService.toSummary(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> users(AuthUser authUser, Role role, String search) {
        accessGuard.requireAdmin(authUser);
        String keyword = search == null ? "" : search;
        List<User> users;
        if (role != null) {
            users = userRepository.findByRoleAndNameContainingIgnoreCase(role, keyword);
        } else {
            users = userRepository.findByNameContainingIgnoreCase(keyword);
        }
        return users.stream().map(this::toMap).toList();
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
        user.setPassword(null);         // 최초 로그인은 birth로 인증
        user.setRole(request.role());
        user.setFamName(request.famName());
        user.setVillageName(request.villageName());
        user.setPasswordChanged(false);
        userRepository.save(user);
        replaceTeams(user.getId(), request.teams(), request.teamRoles());
        return toMap(user);
    }

    public Map<String, Object> update(AuthUser authUser, Long id, AuthDtos.UserUpdateRequest request) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        user.setName(request.name());
        user.setPhone(PhoneUtils.normalize(request.phone()));
        user.setBirth(request.birth());
        user.setFamName(request.famName());
        user.setVillageName(request.villageName());
        return toMap(user);
    }

    public void changeRole(AuthUser authUser, Long id, AuthDtos.RoleUpdateRequest request) {
        organizationService.changeUserRole(authUser, id, request.role());
    }

    public void delete(AuthUser authUser, Long id) {
        accessGuard.requireAdmin(authUser);
        teamRoleRepository.deleteByUserId(id);
        userRepository.delete(getUser(id));
    }

    private void replaceTeams(Long userId, List<String> teams, List<String> teamLeaders) {
        teamRoleRepository.deleteByUserId(userId);
        if (teams == null) return;
        for (String team : teams) {
            boolean isLeader = teamLeaders != null && teamLeaders.contains(team);
            teamRoleRepository.save(new TeamRole(userId, team, isLeader));
        }
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("birth", user.getBirth());
        map.put("role", user.getRole());
        map.put("famName", user.getFamName());
        map.put("villageName", user.getVillageName());
        map.put("summary", authService.toSummary(user));
        return map;
    }
}
