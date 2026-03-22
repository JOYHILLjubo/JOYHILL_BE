package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.KoreanNameGenerator;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.*;
import com.joyhill.demo.repository.FamMemberRepository;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.repository.VillageRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OrganizationService {

    private final VillageRepository villageRepository;
    private final FamRepository famRepository;
    private final FamMemberRepository famMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessGuard accessGuard;

    public OrganizationService(VillageRepository villageRepository, FamRepository famRepository, FamMemberRepository famMemberRepository,
                               UserRepository userRepository, PasswordEncoder passwordEncoder, AccessGuard accessGuard) {
        this.villageRepository = villageRepository;
        this.famRepository = famRepository;
        this.famMemberRepository = famMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                                                .map(this::famMemberMap)
                                                .toList()
                                    );
                                    return famMap;
                                })
                                .toList()
                    );
                    return villageMap;
                })
                .toList();
        return Map.of("villages", villages);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> villages() {
        return villageRepository.findAll().stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", v.getName());
                    map.put("leaderName", v.getLeaderName());
                    return map;
                })
                .toList();
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
        return famRepository.findAll().stream()
                .map(f -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", f.getName());
                    map.put("villageName", f.getVillageName());
                    map.put("leaderName", f.getLeaderName());
                    return map;
                })
                .toList();
    }

    public void moveFamVillage(AuthUser authUser, String famName, AuthDtos.FamVillageUpdateRequest request) {
        Fam fam = famRepository.findByName(famName).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
        if (authUser.role() == Role.village_leader) {
            accessGuard.requireVillageScope(authUser, fam.getVillageName());
            accessGuard.requireVillageScope(authUser, request.toVillage());
        } else {
            accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
        }
        villageRepository.findByName(request.toVillage()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "마을을 찾을 수 없습니다."));
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
    public List<Map<String, Object>> famMembers(AuthUser authUser, String famName) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        return famMemberRepository.findByFamName(famName).stream().map(this::famMemberMap).toList();
    }

    public Map<String, Object> addFamMember(AuthUser authUser, String famName, AuthDtos.FamMemberCreateRequest request) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        Fam fam = famRepository.findByName(famName).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
        FamMember member = new FamMember();
        member.setName(request.name());
        member.setFamName(famName);
        member.setPhone(PhoneUtils.normalize(request.phone()));
        member.setBirth(request.birth());
        member.setRole(request.role() == null ? Role.member : request.role());
        member.setNote(request.note());
        famMemberRepository.save(member);

        if (request.phone() != null && !request.phone().isBlank() && !userRepository.existsByPhone(PhoneUtils.normalize(request.phone()))) {
            User user = new User();
            user.setName(request.name());
            user.setPhone(PhoneUtils.normalize(request.phone()));
            user.setBirth(request.birth() == null ? "000000" : request.birth().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")));
            user.setPassword(passwordEncoder.encode(user.getBirth()));
            user.setRole(member.getRole());
            user.setFamName(famName);
            user.setVillageName(fam.getVillageName());
            userRepository.save(user);
        }
        return famMemberMap(member);
    }

    public Map<String, Object> updateFamMember(AuthUser authUser, Long id, AuthDtos.FamMemberUpdateRequest request) {
        FamMember member = getFamMember(id);
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, member.getFamName());
        member.setName(request.name());
        member.setPhone(PhoneUtils.normalize(request.phone()));
        member.setBirth(request.birth());
        member.setNote(request.note());
        return famMemberMap(member);
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
        User user = userRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Role current = user.getRole();
        if (current == targetRole) {
            return;
        }

        // 강등 체크: 리더 → 팸원 (본인 이름 제외한 팸원이 있으면 강등 불가)
        if (current == Role.leader && targetRole == Role.member) {
            if (famMemberRepository.countByFamNameAndNameNot(user.getFamName(), user.getName()) > 0) {
                throw new ApiException(ErrorCode.DEMOTION_BLOCKED, "팸원이 남아 있어 강등할 수 없습니다.");
            }
        }

        // 강등 체크: 마을장 → 리더 (팸이 있으면 강등 불가)
        if (current == Role.village_leader && targetRole == Role.leader) {
            if (famRepository.countByVillageName(user.getVillageName()) > 0) {
                throw new ApiException(ErrorCode.DEMOTION_BLOCKED, "팸이 남아 있어 강등할 수 없습니다.");
            }
        }

        // 승급: 팸원 → 리더 (팸 자동 생성)
        if (current == Role.member && targetRole == Role.leader) {
            String famName = KoreanNameGenerator.famName(user.getName());
            famRepository.findByName(famName).orElseGet(() -> famRepository.save(new Fam(famName, user.getVillageName(), user.getName())));
            user.setFamName(famName);
        }

        // 승급: 리더 → 마을장 (마을 자동 생성)
        if (current == Role.leader && targetRole == Role.village_leader) {
            String villageName = KoreanNameGenerator.villageName(user.getName());
            villageRepository.findByName(villageName).orElseGet(() -> villageRepository.save(new Village(villageName, user.getName())));
            user.setVillageName(villageName);
        }

        user.setRole(targetRole);
    }

    private FamMember getFamMember(Long id) {
        return famMemberRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸원을 찾을 수 없습니다."));
    }

    private Map<String, Object> famMemberMap(FamMember member) {
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
}
