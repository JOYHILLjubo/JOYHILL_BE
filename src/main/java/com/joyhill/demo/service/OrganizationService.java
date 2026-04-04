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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationService {

    private final VillageRepository villageRepository;
    private final FamRepository famRepository;
    private final FamMemberRepository famMemberRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessGuard accessGuard;

    public OrganizationService(VillageRepository villageRepository, FamRepository famRepository,
                               FamMemberRepository famMemberRepository, UserRepository userRepository,
                               AttendanceRepository attendanceRepository,
                               AccessGuard accessGuard) {
        this.villageRepository = villageRepository;
        this.famRepository = famRepository;
        this.famMemberRepository = famMemberRepository;
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
                                famMap.put("members", famMemberRepository.findByFamName(fam.getName()).stream()
                                        .map(this::famMemberBasicMap).toList());
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
        userRepository.findAll().stream()
                .filter(user -> famName.equals(user.getFamName()))
                .forEach(user -> user.setVillageName(request.toVillage()));
    }

    public void deleteFam(AuthUser authUser, String famName) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (famMemberRepository.countByFamName(famName) > 0) {
            throw new ApiException(ErrorCode.DELETION_BLOCKED, "팸원이 남아 있어 팸을 삭제할 수 없습니다.");
        }
        famRepository.deleteByName(famName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> famMembers(AuthUser authUser, String famName, String period) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        List<FamMember> members = famMemberRepository.findByFamName(famName);

        LocalDate to = LocalDate.now();
        LocalDate from = parsePeriod(period, to);

        List<Long> memberIds = members.stream().map(FamMember::getId).toList();
        List<com.joyhill.demo.domain.Attendance> attendances =
                attendanceRepository.findByFamMemberIdInAndDateBetween(memberIds, from, to);

        Map<Long, List<com.joyhill.demo.domain.Attendance>> byMember = attendances.stream()
                .collect(Collectors.groupingBy(com.joyhill.demo.domain.Attendance::getFamMemberId));

        return members.stream().map(m -> famMemberMapWithRate(m, byMember.getOrDefault(m.getId(), List.of()))).toList();
    }

    public Map<String, Object> addFamMember(AuthUser authUser, String famName, AuthDtos.FamMemberCreateRequest request) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        Fam fam = famRepository.findByName(famName)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
        FamMember member = new FamMember();
        member.setName(request.name());
        member.setFamName(famName);
        member.setPhone(PhoneUtils.normalize(request.phone()));
        member.setBirth(request.birth());
        member.setRole(request.role() == null ? Role.member : request.role());
        member.setNote(request.note());
        famMemberRepository.save(member);

        // 전화번호가 있고 기존 계정이 없으면 User 자동 생성
        if (request.phone() != null && !request.phone().isBlank()
                && !userRepository.existsByPhone(PhoneUtils.normalize(request.phone()))) {
            User user = new User();
            user.setName(request.name());
            user.setPhone(PhoneUtils.normalize(request.phone()));
            String birth = request.birth() == null ? "000000"
                    : request.birth().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            user.setBirth(birth);
            user.setPassword(null);         // 최초 로그인은 birth로 인증
            user.setRole(member.getRole());
            user.setFamName(famName);
            user.setVillageName(fam.getVillageName());
            user.setPasswordChanged(false);
            userRepository.save(user);
        }
        return famMemberBasicMap(member);
    }

    public Map<String, Object> updateFamMember(AuthUser authUser, Long id, AuthDtos.FamMemberUpdateRequest request) {
        FamMember member = getFamMember(id);
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, member.getFamName());
        member.setName(request.name());
        member.setPhone(PhoneUtils.normalize(request.phone()));
        member.setBirth(request.birth());
        member.setNote(request.note());
        return famMemberBasicMap(member);
    }

    public void updateFamMemberRole(AuthUser authUser, Long id, AuthDtos.RoleUpdateRequest request) {
        FamMember member = getFamMember(id);
        accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
        accessGuard.requireFamScope(authUser, member.getFamName());
        member.setRole(request.role());
    }

    public void deleteFamMember(AuthUser authUser, Long id) {
        FamMember member = getFamMember(id);
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, member.getFamName());
        famMemberRepository.delete(member);
    }

    public void changeUserRole(AuthUser authUser, Long id, Role targetRole) {
        accessGuard.requireAdmin(authUser);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Role current = user.getRole();
        if (current == targetRole) return;

        if (current == Role.leader && targetRole == Role.member) {
            if (famMemberRepository.countByFamNameAndNameNot(user.getFamName(), user.getName()) > 0) {
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

    private LocalDate parsePeriod(String period, LocalDate to) {
        return switch (period == null ? "1month" : period) {
            case "3month" -> to.minusMonths(3);
            case "6month" -> to.minusMonths(6);
            default -> to.minusMonths(1);
        };
    }

    private FamMember getFamMember(Long id) {
        return famMemberRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸원을 찾을 수 없습니다."));
    }

    private Map<String, Object> famMemberBasicMap(FamMember member) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", member.getId());
        map.put("name", member.getName());
        map.put("famName", member.getFamName());
        map.put("phone", member.getPhone());
        map.put("birth", member.getBirth());
        map.put("role", member.getRole());
        map.put("note", member.getNote());
        return map;
    }

    private Map<String, Object> famMemberMapWithRate(FamMember member,
                                                      List<com.joyhill.demo.domain.Attendance> records) {
        Map<String, Object> map = famMemberBasicMap(member);
        int total = records.size();
        if (total == 0) {
            map.put("worshipRate", 0);
            map.put("famRate", 0);
        } else {
            long worship = records.stream().filter(com.joyhill.demo.domain.Attendance::isWorshipPresent).count();
            long fam = records.stream().filter(com.joyhill.demo.domain.Attendance::isFamPresent).count();
            map.put("worshipRate", (int) Math.round((double) worship / total * 100));
            map.put("famRate", (int) Math.round((double) fam / total * 100));
        }
        return map;
    }
}
