package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.KoreanNameGenerator;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.*;
import com.joyhill.demo.repository.*;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationService {

    private final VillageRepository villageRepository;
    private final FamRepository famRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessGuard accessGuard;

    public OrganizationService(VillageRepository villageRepository,
                               FamRepository famRepository,
                               UserRepository userRepository,
                               AttendanceRepository attendanceRepository,
                               AccessGuard accessGuard) {
        this.villageRepository = villageRepository;
        this.famRepository = famRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> structure() {
        var villages = villageRepository.findAll().stream()
                .sorted(Comparator.comparing(Village::getName))
                .map(village -> {
                    Map<String, Object> villageMap = new HashMap<>();
                    villageMap.put("name", village.getName());
                    villageMap.put("leaderName", village.getLeaderName());
                    villageMap.put("fams", famRepository.findByVillageName(village.getName()).stream()
                            .sorted(Comparator.comparing(Fam::getName))
                            .map(fam -> {
                                Map<String, Object> famMap = new HashMap<>();
                                famMap.put("name", fam.getName());
                                famMap.put("leaderName", fam.getLeaderName());
                                famMap.put("members", userRepository.findByFamName(fam.getName()).stream()
                                        .map(this::userBasicMap).toList());
                                return famMap;
                            }).toList());
                    return villageMap;
                }).toList();
        return Map.of("villages", villages);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> villages() {
        return villageRepository.findAll().stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", v.getName());
            map.put("leaderName", v.getLeaderName());
            return map;
        }).toList();
    }

    public Map<String, Object> createVillage(AuthUser authUser, AuthDtos.VillageCreateRequest request) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (villageRepository.findByName(request.name()).isPresent()) {
            throw new ApiException(ErrorCode.DUPLICATE_NAME, "이미 존재하는 마을 이름입니다.");
        }
        Village village = villageRepository.save(new Village(request.name(), request.leaderName()));
        return Map.of("name", village.getName(), "leaderName", village.getLeaderName() == null ? "" : village.getLeaderName());
    }

    public void deleteVillage(AuthUser authUser, String villageName) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (famRepository.countByVillageName(villageName) > 0) {
            throw new ApiException(ErrorCode.DELETION_BLOCKED, "팸이 남아 있어 마을을 삭제할 수 없습니다.");
        }
        villageRepository.deleteByName(villageName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> fams() {
        return famRepository.findAll().stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", f.getName());
            map.put("villageName", f.getVillageName());
            map.put("leaderName", f.getLeaderName());
            return map;
        }).toList();
    }

    public Map<String, Object> createFam(AuthUser authUser, AuthDtos.FamCreateRequest request) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (famRepository.findByName(request.name()).isPresent()) {
            throw new ApiException(ErrorCode.DUPLICATE_NAME, "이미 존재하는 팸 이름입니다.");
        }
        villageRepository.findByName(request.villageName())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "마을을 찾을 수 없습니다."));
        Fam fam = famRepository.save(new Fam(request.name(), request.villageName(), request.leaderName()));
        return Map.of("name", fam.getName(), "villageName", fam.getVillageName(),
                "leaderName", fam.getLeaderName() == null ? "" : fam.getLeaderName());
    }

    public void moveFamVillage(AuthUser authUser, String famName, AuthDtos.FamVillageUpdateRequest request) {
        Fam fam = famRepository.findByName(famName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
        if (authUser.role() == Role.village_leader) {
            accessGuard.requireVillageScope(authUser, fam.getVillageName());
            accessGuard.requireVillageScope(authUser, request.toVillage());
        } else {
            accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
        }
        villageRepository.findByName(request.toVillage())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "마을을 찾을 수 없습니다."));
        fam.setVillageName(request.toVillage());
        userRepository.findByFamName(famName)
                .forEach(user -> user.setVillageName(request.toVillage()));
    }

    public void deleteFam(AuthUser authUser, String famName) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (userRepository.countByFamName(famName) > 0) {
            throw new ApiException(ErrorCode.DELETION_BLOCKED, "팸원이 남아 있어 팸을 삭제할 수 없습니다.");
        }
        famRepository.deleteByName(famName);
    }

    /**
     * 팸원 목록 (출석률 포함)
     * @param year 연도 지정 시 해당 연도 1/1 ~ 12/31 (현재 연도면 오늘까지), null이면 전체 기간
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> famMembers(AuthUser authUser, String famName, Integer year) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);

        List<User> members = userRepository.findByFamName(famName);

        LocalDate from;
        LocalDate to;

        if (year != null) {
            int currentYear = LocalDate.now().getYear();
            from = LocalDate.of(year, 1, 1);
            to = (year == currentYear) ? LocalDate.now() : LocalDate.of(year, 12, 31);
        } else {
            from = LocalDate.of(2000, 1, 1);
            to = LocalDate.now();
        }

        List<Long> userIds = members.stream().map(User::getId).toList();
        List<Attendance> attendances = attendanceRepository.findByUserIdInAndDateBetween(userIds, from, to);

        Map<Long, List<Attendance>> byUser = attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        return members.stream()
                .map(m -> userMapWithRate(m, byUser.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    public Map<String, Object> addFamMember(AuthUser authUser, String famName, AuthDtos.FamMemberCreateRequest request) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);

        Fam fam = famRepository.findByName(famName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));

        if (request.phone() != null && !request.phone().isBlank()
                && userRepository.existsByPhone(PhoneUtils.normalize(request.phone()))) {
            throw new ApiException(ErrorCode.DUPLICATE_PHONE, "이미 등록된 전화번호입니다.");
        }

        User user = new User();
        user.setName(request.name());
        user.setPhone(request.phone() != null && !request.phone().isBlank()
                ? PhoneUtils.normalize(request.phone()) : generateTempPhone());
        user.setRole(request.role() == null ? Role.member : request.role());
        user.setFamName(famName);
        user.setVillageName(fam.getVillageName());
        user.setNote(request.note());
        user.setPasswordChanged(false);
        user.setPassword(null);

        String birth = "000000";
        if (request.birth() != null) {
            birth = request.birth().format(DateTimeFormatter.ofPattern("yyMMdd"));
        }
        user.setBirth(birth);

        userRepository.save(user);
        return userBasicMap(user);
    }

    public Map<String, Object> updateFamMember(AuthUser authUser, Long id, AuthDtos.FamMemberUpdateRequest request) {
        User user = getUser(id);
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, user.getFamName());
        user.setName(request.name());
        if (request.phone() != null) user.setPhone(PhoneUtils.normalize(request.phone()));
        if (request.birth() != null) {
            user.setBirth(request.birth().format(DateTimeFormatter.ofPattern("yyMMdd")));
        }
        user.setNote(request.note());
        return userBasicMap(user);
    }

    public void updateFamMemberRole(AuthUser authUser, Long id, AuthDtos.RoleUpdateRequest request) {
        User user = getUser(id);
        accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
        accessGuard.requireFamScope(authUser, user.getFamName());
        user.setRole(request.role());
    }

    public void deleteFamMember(AuthUser authUser, Long id) {
        User user = getUser(id);
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, user.getFamName());
        userRepository.delete(user);
    }

    public void changeUserRole(AuthUser authUser, Long id, Role targetRole) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        Role current = user.getRole();
        if (current == targetRole) return;

        if (current == Role.leader && targetRole == Role.member) {
            if (userRepository.countByFamNameAndNameNot(user.getFamName(), user.getName()) > 0) {
                throw new ApiException(ErrorCode.DEMOTION_BLOCKED, "팸원이 남아 있어 강등할 수 없습니다.");
            }
        }
        if (current == Role.village_leader && targetRole == Role.leader) {
            if (famRepository.countByVillageName(user.getVillageName()) > 0) {
                throw new ApiException(ErrorCode.DEMOTION_BLOCKED, "팸이 남아 있어 강등할 수 없습니다.");
            }
        }
        if (current == Role.member && targetRole == Role.leader) {
            String famName = KoreanNameGenerator.famName(user.getName());
            famRepository.findByName(famName).orElseGet(() ->
                    famRepository.save(new Fam(famName, user.getVillageName(), user.getName())));
            user.setFamName(famName);
        }
        if (current == Role.leader && targetRole == Role.village_leader) {
            String villageName = KoreanNameGenerator.villageName(user.getName());
            villageRepository.findByName(villageName).orElseGet(() ->
                    villageRepository.save(new Village(villageName, user.getName())));
            user.setVillageName(villageName);
        }
        user.setRole(targetRole);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸원을 찾을 수 없습니다."));
    }

    private String generateTempPhone() {
        return "000-" + System.currentTimeMillis() % 100000000;
    }

    private Map<String, Object> userBasicMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("famName", user.getFamName());
        map.put("phone", user.getPhone());
        map.put("birth", user.getBirth());
        map.put("role", user.getRole());
        map.put("note", user.getNote());
        return map;
    }

    private Map<String, Object> userMapWithRate(User user, List<Attendance> records) {
        Map<String, Object> map = userBasicMap(user);
        int total = records.size();
        if (total == 0) {
            map.put("worshipRate", 0);
            map.put("famRate", 0);
        } else {
            long worship = records.stream().filter(Attendance::isWorshipPresent).count();
            long fam = records.stream().filter(Attendance::isFamPresent).count();
            map.put("worshipRate", (int) Math.round((double) worship / total * 100));
            map.put("famRate", (int) Math.round((double) fam / total * 100));
        }
        return map;
    }
}
