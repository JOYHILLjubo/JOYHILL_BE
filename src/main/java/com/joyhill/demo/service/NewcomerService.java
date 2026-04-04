package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.Newcomer;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.NewcomerRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NewcomerService {

    private final NewcomerRepository newcomerRepository;
    private final FamRepository famRepository;
    private final UserRepository userRepository;
    private final AccessGuard accessGuard;

    public NewcomerService(NewcomerRepository newcomerRepository,
                           FamRepository famRepository,
                           UserRepository userRepository,
                           AccessGuard accessGuard) {
        this.newcomerRepository = newcomerRepository;
        this.famRepository = famRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser) {
        accessGuard.requireNewcomerManager(authUser);
        return newcomerRepository.findAll().stream().map(this::toMap).toList();
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.NewcomerRequest request) {
        accessGuard.requireNewcomerManager(authUser);
        Newcomer newcomer = new Newcomer();
        newcomer.setName(request.name());
        newcomer.setPhone(PhoneUtils.normalize(request.phone()));
        newcomer.setBirth(request.birth());
        newcomer.setRegisteredAt(request.registeredAt());
        newcomer.setNote(request.note());
        newcomerRepository.save(newcomer);
        return toMap(newcomer);
    }

    /**
     * 팸 배정: newcomer → users 테이블로 이동 후 newcomer 삭제
     * 이 시점에 앱 계정이 생성되어 로그인 가능해짐
     */
    public Map<String, Object> assignFam(AuthUser authUser, Long id, AuthDtos.NewcomerFamAssignRequest request) {
        accessGuard.requireRoleAtLeast(authUser, Role.leader);
        accessGuard.requireFamScope(authUser, request.famName());

        var fam = famRepository.findByName(request.famName())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));

        Newcomer newcomer = getNewcomer(id);

        // 전화번호 중복 체크 (이미 user 계정 있는 경우)
        String normalizedPhone = PhoneUtils.normalize(newcomer.getPhone());
        if (normalizedPhone != null && !normalizedPhone.isBlank()
                && userRepository.existsByPhone(normalizedPhone)) {
            throw new ApiException(ErrorCode.DUPLICATE_PHONE, "이미 계정이 존재하는 전화번호입니다.");
        }

        // newcomer → user 생성
        User user = new User();
        user.setName(newcomer.getName());
        user.setPhone(normalizedPhone != null && !normalizedPhone.isBlank()
                ? normalizedPhone : generateTempPhone());
        user.setRole(Role.member);
        user.setFamName(fam.getName());
        user.setVillageName(fam.getVillageName());
        user.setNote(newcomer.getNote());
        user.setPasswordChanged(false);
        user.setPassword(null);

        // birth 처리 (newcomer.birth는 LocalDate, user.birth는 String yyMMdd)
        String birth = "000000";
        if (newcomer.getBirth() != null) {
            birth = newcomer.getBirth().format(DateTimeFormatter.ofPattern("yyMMdd"));
        }
        user.setBirth(birth);

        userRepository.save(user);

        // newcomer 삭제 (users로 이동 완료)
        newcomerRepository.delete(newcomer);

        return Map.of(
                "message", newcomer.getName() + "님이 " + fam.getName() + "에 배정되었습니다.",
                "userId", user.getId(),
                "famName", fam.getName()
        );
    }

    public void delete(AuthUser authUser, Long id) {
        accessGuard.requireRoleAtLeast(authUser, Role.leader);
        newcomerRepository.delete(getNewcomer(id));
    }

    private Newcomer getNewcomer(Long id) {
        return newcomerRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "새가족을 찾을 수 없습니다."));
    }

    private String generateTempPhone() {
        return "000-" + System.currentTimeMillis() % 100000000;
    }

    private Map<String, Object> toMap(Newcomer newcomer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", newcomer.getId());
        map.put("name", newcomer.getName());
        map.put("phone", newcomer.getPhone());
        map.put("birth", newcomer.getBirth());
        map.put("registeredAt", newcomer.getRegisteredAt());
        map.put("note", newcomer.getNote());
        map.put("famName", newcomer.getFamName());
        map.put("createdAt", newcomer.getCreatedAt());
        return map;
    }
}
