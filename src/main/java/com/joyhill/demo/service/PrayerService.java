package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Prayer;
import com.joyhill.demo.domain.PrayerType;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.repository.PrayerRepository;
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
public class PrayerService {

    // 교역자 전용 기도 공간 식별자
    private static final String PASTOR_FAM = "__pastor__";

    private final PrayerRepository prayerRepository;
    private final UserRepository userRepository;
    private final AccessGuard accessGuard;

    public PrayerService(PrayerRepository prayerRepository, UserRepository userRepository, AccessGuard accessGuard) {
        this.prayerRepository = prayerRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser, String famName, int year, int month, Integer week) {
        // 교역자: 교역자 전용 공간만 조회 가능
        if (authUser.role() == Role.pastor) {
            List<Prayer> prayers = week == null
                    ? prayerRepository.findByFamNameAndYearAndMonth(PASTOR_FAM, year, month)
                    : prayerRepository.findByFamNameAndYearAndMonthAndWeek(PASTOR_FAM, year, month, week);
            return prayers.stream().map(this::toMap).toList();
        }

        // 관리자: 기도 탭 없음 → 접근 차단
        if (authUser.role() == Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "관리자는 기도제목에 접근할 수 없습니다.");
        }

        // 팸원/리더/마을장: 본인 팸 기도제목
        String targetFam = famName == null ? authUser.famName() : famName;
        accessGuard.requireFamScope(authUser, targetFam);
        List<Prayer> prayers = week == null
                ? prayerRepository.findByFamNameAndYearAndMonth(targetFam, year, month)
                : prayerRepository.findByFamNameAndYearAndMonthAndWeek(targetFam, year, month, week);
        return prayers.stream().map(this::toMap).toList();
    }

    public Map<String, Object> createPersonal(AuthUser authUser, AuthDtos.PrayerRequest request) {
        // 관리자: 접근 차단
        if (authUser.role() == Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "관리자는 기도제목에 접근할 수 없습니다.");
        }

        // 교역자: 교역자 전용 공간에 저장
        String targetFam = authUser.role() == Role.pastor ? PASTOR_FAM : request.famName();
        if (authUser.role() != Role.pastor) {
            accessGuard.requireFamScope(authUser, targetFam);
        }

        Prayer prayer = new Prayer();
        prayer.setUserId(authUser.userId());
        prayer.setFamName(targetFam);
        prayer.setType(PrayerType.personal);
        prayer.setContent(request.content());
        prayer.setYear(request.year());
        prayer.setMonth(request.month());
        prayer.setWeek(request.week());
        prayerRepository.save(prayer);
        return toMap(prayer);
    }

    public Map<String, Object> updatePersonal(AuthUser authUser, Long id, AuthDtos.PrayerRequest request) {
        Prayer prayer = getPrayer(id);
        if (!authUser.userId().equals(prayer.getUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 기도제목만 수정할 수 있습니다.");
        }
        prayer.setContent(request.content());
        // 교역자는 famName 고정
        if (authUser.role() != Role.pastor) {
            prayer.setFamName(request.famName());
        }
        prayer.setYear(request.year());
        prayer.setMonth(request.month());
        prayer.setWeek(request.week());
        return toMap(prayer);
    }

    public Map<String, Object> upsertCommon(AuthUser authUser, AuthDtos.CommonPrayerRequest request) {
        // 교역자/관리자: 팸 공동 기도제목 작성 불가
        if (authUser.role() == Role.pastor || authUser.role() == Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "교역자/관리자는 팸 공동 기도제목을 작성할 수 없습니다.");
        }
        accessGuard.requireRoleAtLeast(authUser, Role.leader);
        accessGuard.requireFamScope(authUser, request.famName());
        Prayer prayer = prayerRepository.findByFamNameAndYearAndMonthAndType(
                request.famName(), request.year(), request.month(), PrayerType.common)
                .orElseGet(Prayer::new);
        prayer.setUserId(authUser.userId());
        prayer.setFamName(request.famName());
        prayer.setType(PrayerType.common);
        prayer.setContent(request.content());
        prayer.setYear(request.year());
        prayer.setMonth(request.month());
        prayer.setWeek(null);
        prayerRepository.save(prayer);
        return toMap(prayer);
    }

    private Prayer getPrayer(Long id) {
        return prayerRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "기도제목을 찾을 수 없습니다."));
    }

    private Map<String, Object> toMap(Prayer prayer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", prayer.getId());
        map.put("userId", prayer.getUserId());
        map.put("famName", prayer.getFamName());
        map.put("type", prayer.getType());
        map.put("content", prayer.getContent());
        map.put("year", prayer.getYear());
        map.put("month", prayer.getMonth());
        map.put("week", prayer.getWeek());
        String name = userRepository.findById(prayer.getUserId())
                .map(user -> user.getName())
                .orElse("");
        map.put("name", name);
        return map;
    }
}
