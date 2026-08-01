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

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Locale;
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
                                        .sorted(Comparator.comparing(User::getName, Comparator.nullsLast(Collator.getInstance(Locale.KOREAN)::compare)))
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

        boolean isCurrentYear = (targetYear == LocalDate.now().getYear());
        return members.stream()
                .sorted(Comparator.comparing(User::getName, Comparator.nullsLast(Collator.getInstance(Locale.KOREAN)::compare)))
                .map(m -> userMapWithRate(m, byUser.getOrDefault(m.getId(), List.of()), totalSundays, isCurrentYear))
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
        if (request.birth() != null && !request.birth().isBlank()) {
            birth = request.birth().replaceAll("\\D", "");
            if (birth.length() > 6) birth = birth.substring(birth.length() - 6); // YYYYMMDD → YYMMDD
            birth = birth.substring(0, Math.min(6, birth.length()));
        }
        user.setBirth(birth);
        userRepository.save(user);
        return userBasicMap(user);
    }

    public Map<String, Object> updateFamMember(AuthUser authUser, Long id, AuthDtos.FamMemberUpdateRequest request) {
        User user = getUser(id);
        accessGuard.requireLeader(authUser);
        boolean isPastorOrAdmin = authUser.role() == Role.pastor || authUser.role() == Role.admin;
        // 미배정 인원을 검색해서 본인 팸으로 편입시키는 경우: leader도 허용
        boolean leaderClaimingUnassigned = authUser.role() == Role.leader
                && user.getFamName() == null
                && request.famName() != null
                && request.famName().equals(authUser.famName());
        if (user.getFamName() != null) {
            accessGuard.requireFamScope(authUser, user.getFamName());
        } else if (!isPastorOrAdmin && !leaderClaimingUnassigned) {
            accessGuard.requirePastorOrAdmin(authUser);
        }
        if (request.name() != null && !request.name().isBlank()) user.setName(request.name());
        if (request.phone() != null) {
            String normalizedPhone = PhoneUtils.normalize(request.phone());
            if (userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
                throw new ApiException(ErrorCode.DUPLICATE_PHONE, "이미 등록된 전화번호입니다.");
            }
            user.setPhone(normalizedPhone);
        }
        if (request.birth() != null && !request.birth().isBlank()) {
            String birthDigits = request.birth().replaceAll("\\D", "");
            // YYYYMMDD(8자리) 형태면 앞 2자리(세기) 제거 → YYMMDD(6자리)
            String birth6 = birthDigits.length() == 8
                    ? birthDigits.substring(2)
                    : birthDigits.substring(0, Math.min(6, birthDigits.length()));
            user.setBirth(birth6);
        }
        user.setNote(request.note());
        // 팸 변경 처리: pastor/admin은 임의 이동, leader는 미배정 인원을 본인 팸으로 편입만 가능
        if (request.famName() != null && (isPastorOrAdmin || leaderClaimingUnassigned)) {
            String newFamName = request.famName().isBlank() ? null : request.famName();
            user.setFamName(newFamName);
            if (newFamName != null) {
                famRepository.findByName(newFamName).ifPresent(fam -> user.setVillageName(fam.getVillageName()));
            } else {
                user.setVillageName(null);
            }
        }
        return userBasicMap(user);
    }

    public void updateFamMemberRole(AuthUser authUser, Long id, AuthDtos.RoleUpdateRequest request) {
        User user = getUser(id);
        accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
        if (user.getFamName() != null) {
            accessGuard.requireFamScope(authUser, user.getFamName());
        } else {
            accessGuard.requirePastorOrAdmin(authUser);
        }
        user.setRole(request.role());
    }

    public void deleteFamMember(AuthUser authUser, Long id) {
        User user = getUser(id);
        accessGuard.requireLeader(authUser);
        if (user.getFamName() != null) {
            accessGuard.requireFamScope(authUser, user.getFamName());
        } else {
            accessGuard.requirePastorOrAdmin(authUser);
        }
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
        map.put("phone", PhoneUtils.format(user.getPhone()));
        map.put("birth", user.getBirth());
        map.put("role", user.getRole());
        map.put("note", user.getNote());
        map.put("avatarKey", user.getAvatarKey());
        map.put("avatarPhotoUrl", user.getAvatarPhotoUrl());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> unassignedMembers(AuthUser authUser) {
        accessGuard.requirePastorOrAdmin(authUser);
        return userRepository.findByFamNameIsNull().stream()
                .filter(u -> u.getRole() == Role.member || u.getRole() == Role.leader)
                .sorted(Comparator.comparing(User::getName))
                .map(this::userBasicMap)
                .toList();
    }

    private Map<String, Object> userMapWithRate(User user, List<Attendance> records, int totalSundays, boolean isCurrentYear) {
        Map<String, Object> map = userBasicMap(user);

        long worship = records.stream().filter(Attendance::isWorshipPresent).count();
        long online  = records.stream().filter(Attendance::isOnlinePresent).count();
        long fam     = records.stream().filter(Attendance::isFamPresent).count();

        if (isCurrentYear) {
            // 분모 = 올해 총 주차 수로 통일, 분자만 legacy + 신규 합산
            // 이렇게 해야 16주 전체 개근 시 100% 방지차없이 달성 가능
            long worshipAtt = worship + user.getLegacyWorshipAttended();
            long famAtt     = fam     + user.getLegacyFamAttended();

            map.put("worshipRate", totalSundays == 0 ? 0 : (int) Math.round((double) worshipAtt / totalSundays * 100));
            map.put("onlineRate",  totalSundays == 0 ? 0 : (int) Math.round((double) online     / totalSundays * 100));
            map.put("famRate",     totalSundays == 0 ? 0 : (int) Math.round((double) famAtt     / totalSundays * 100));
        } else {
            map.put("worshipRate", totalSundays == 0 ? 0 : (int) Math.round((double) worship / totalSundays * 100));
            map.put("onlineRate",  totalSundays == 0 ? 0 : (int) Math.round((double) online  / totalSundays * 100));
            map.put("famRate",     totalSundays == 0 ? 0 : (int) Math.round((double) fam     / totalSundays * 100));
        }
        return map;
    }
}
