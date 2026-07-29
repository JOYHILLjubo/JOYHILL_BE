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
import java.time.LocalDate;
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

    // User.birth는 LocalDate가 아니라 "YYMMDD" 6자리 문자열이라 자바에서 직접 파싱해서 이번 달 생일자를 거른다.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> birthdaysThisMonth() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.admin)
                .filter(u -> u.getBirth() != null && u.getBirth().length() == 6)
                .filter(u -> parseBirthMonth(u.getBirth()) == currentMonth)
                .sorted(Comparator.comparingInt(u -> birthdaySortKey(parseBirthDay(u.getBirth()), currentDay)))
                .map(u -> toBirthdayMap(u, currentDay))
                .toList();
    }

    // 아직 안 지난 사람(오늘 포함)을 가까운 순서대로 앞에, 이미 지난 사람은 뒤에(최근에 지난 사람부터) 정렬하기 위한 키
    private int birthdaySortKey(int day, int currentDay) {
        return day >= currentDay ? (day - currentDay) : (1000 + (currentDay - day));
    }

    private int parseBirthMonth(String birth) {
        return Integer.parseInt(birth.substring(2, 4));
    }

    private int parseBirthDay(String birth) {
        return Integer.parseInt(birth.substring(4, 6));
    }

    private Map<String, Object> toBirthdayMap(User user, int currentDay) {
        int day = parseBirthDay(user.getBirth());
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("famName", user.getFamName());
        map.put("villageName", user.getVillageName());
        map.put("avatarKey", user.getAvatarKey());
        map.put("day", day);
        map.put("isToday", day == currentDay);
        return map;
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
        user.setBirth(sanitizeBirth(request.birth()));
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
        if (request.phone() != null) {
            String normalizedPhone = PhoneUtils.normalize(request.phone());
            if (userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
                throw new ApiException(ErrorCode.DUPLICATE_PHONE, "이미 등록된 전화번호입니다.");
            }
            user.setPhone(normalizedPhone);
        }
        if (request.birth() != null) user.setBirth(sanitizeBirth(request.birth()));
        if (request.famName() != null) user.setFamName(request.famName());
        if (request.villageName() != null) user.setVillageName(request.villageName());
        return toMap(user);
    }

    // 숫자 외 문자 제거 후 YYMMDD(6자리)로 정규화 — 여기서 걸러두지 않으면 잘못된 형식이
    // birthdaysThisMonth()의 Integer.parseInt에서 전체 목록 조회를 통째로 500으로 만듦
    private String sanitizeBirth(String birth) {
        if (birth == null || birth.isBlank()) return birth;
        String digits = birth.replaceAll("\\D", "");
        return digits.length() == 8 ? digits.substring(2) : digits.substring(0, Math.min(6, digits.length()));
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

    public void updateAvatar(AuthUser authUser, String avatarKey) {
        User user = getUser(authUser.userId());
        user.setAvatarKey(avatarKey);
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
