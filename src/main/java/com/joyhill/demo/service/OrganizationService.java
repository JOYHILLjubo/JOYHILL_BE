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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
     * 팸원 목록 + 출석률
     * 출석률 기준: 해당 연도 1월 첫째 주 일요일 ~ 오늘(현재 연도) or 12/31(과거 연도)
     * 분모: 전체 일요일 수 (주 단위)
     * 분자: 출석한 일요일 수
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> famMembers(AuthUser authUser, String famName, Integer year) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);

        List<User> members = userRepository.findByFamName(famName);

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        LocalDate from = LocalDate.of(targetYear, 1, 1);
        LocalDate to = (targetYear == LocalDate.now().getYear()) ? LocalDate.now() : LocalDate.of(targetYear, 12, 31);

        // 해당 기간의 전체 일요일 수 (출석률 분모)
        int totalSundays = countSundaysInPeriod(from, to);

        List<Long> userIds = members.stream().map(User::getId).toList();
        List<Attendance> attendances = attendanceRepository.findByUserIdInAndDateBetween(userIds, from, to);

        Map<Long, List<Attendance>> byUser = attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        return members.stream()
                .map(m -> userMapWithRate(m, byUser.getOrDefault(m.getId(), List.of()), totalSundays))
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

    /**
     * 역할 변경 (승진/강등)
     *
     * 팸원 → 리더:
     *   기존 팸에서 빠지고, {이름}팸 생성, 해당 팸의 리더로 등록
     *
     * 리더 → 마을장:
     *   기존 팸({이름}팸)을 새 마을({이름}네)로 이동 (팸은 유지, 마을 귀속만 변경)
     *   마을장은 기존 팸에 소속 유지
     */
    public void changeUserRole(AuthUser authUser, Long id, Role targetRole) {
        accessGuard.requireAdmin(authUser);
        User user = getUser(id);
        Role current = user.getRole();
        if (current == targetRole) return;

        // ── 강등 가드 ──
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

        // ── 팸원 → 리더 ──
        if (current == Role.member && targetRole == Role.leader) {
            String newFamName = KoreanNameGenerator.famName(user.getName());
            Fam newFam = famRepository.findByName(newFamName).orElseGet(() ->
                    famRepository.save(new Fam(newFamName, user.getVillageName(), user.getName())));
            // 리더 이름 업데이트
            newFam.setLeaderName(user.getName());
            // 기존 팸에서 나오고 새 팸으로 이동
            user.setFamName(newFamName);
        }

        // ── 리더 → 마을장 ──
        if (current == Role.leader && targetRole == Role.village_leader) {
            String newVillageName = KoreanNameGenerator.villageName(user.getName());
            Village newVillage = villageRepository.findByName(newVillageName).orElseGet(() ->
                    villageRepository.save(new Village(newVillageName, user.getName())));
            newVillage.setLeaderName(user.getName());

            // 기존 팸을 새 마을로 이동 (팸 유지, 마을 귀속만 변경)
            String existingFamName = user.getFamName();
            if (existingFamName != null) {
                famRepository.findByName(existingFamName).ifPresent(fam ->
                        fam.setVillageName(newVillageName));
                // 팸 소속 모든 유저의 villageName 업데이트
                userRepository.findByFamName(existingFamName)
                        .forEach(member -> member.setVillageName(newVillageName));
            }

            user.setVillageName(newVillageName);
            // famName은 유지 → 마을장도 기존 팸에 소속
        }

        user.setRole(targetRole);
    }

    // ── 내부 유틸 ──

    /**
     * 주어진 기간 내 일요일 수 계산 (출석률 분모)
     * from의 첫 번째 일요일부터 to까지
     */
    private int countSundaysInPeriod(LocalDate from, LocalDate to) {
        LocalDate firstSunday = from;
        while (firstSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            firstSunday = firstSunday.plusDays(1);
        }
        if (firstSunday.isAfter(to)) return 0;
        return (int) ChronoUnit.WEEKS.between(firstSunday, to) + 1;
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

    private Map<String, Object> userMapWithRate(User user, List<Attendance> records, int totalSundays) {
        Map<String, Object> map = userBasicMap(user);
        if (totalSundays == 0) {
            map.put("worshipRate", 0);
            map.put("famRate", 0);
        } else {
            // 분모: 전체 일요일 수 / 분자: 출석한 일요일 수
            long worship = records.stream().filter(Attendance::isWorshipPresent).count();
            long fam = records.stream().filter(Attendance::isFamPresent).count();
            map.put("worshipRate", (int) Math.round((double) worship / totalSundays * 100));
            map.put("famRate", (int) Math.round((double) fam / totalSundays * 100));
        }
        return map;
    }
}
