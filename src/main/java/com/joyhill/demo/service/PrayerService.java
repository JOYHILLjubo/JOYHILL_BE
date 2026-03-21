package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Prayer;
import com.joyhill.demo.domain.PrayerType;
import com.joyhill.demo.repository.PrayerRepository;
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

    private final PrayerRepository prayerRepository;
    private final AccessGuard accessGuard;

    public PrayerService(PrayerRepository prayerRepository, AccessGuard accessGuard) {
        this.prayerRepository = prayerRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser, String famName, int year, int month, Integer week) {
        accessGuard.requireFamScope(authUser, famName == null ? authUser.famName() : famName);
        String targetFam = famName == null ? authUser.famName() : famName;
        List<Prayer> prayers = week == null
                ? prayerRepository.findByFamNameAndYearAndMonth(targetFam, year, month)
                : prayerRepository.findByFamNameAndYearAndMonthAndWeek(targetFam, year, month, week);
        return prayers.stream().map(this::toMap).toList();
    }

    public Map<String, Object> createPersonal(AuthUser authUser, AuthDtos.PrayerRequest request) {
        accessGuard.requireFamScope(authUser, request.famName());
        Prayer prayer = new Prayer();
        prayer.setUserId(authUser.userId());
        prayer.setFamName(request.famName());
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
        prayer.setFamName(request.famName());
        prayer.setYear(request.year());
        prayer.setMonth(request.month());
        prayer.setWeek(request.week());
        return toMap(prayer);
    }

    public Map<String, Object> upsertCommon(AuthUser authUser, AuthDtos.CommonPrayerRequest request) {
        accessGuard.requireRoleAtLeast(authUser, com.joyhill.demo.domain.Role.leader);
        accessGuard.requireFamScope(authUser, request.famName());
        Prayer prayer = prayerRepository.findByFamNameAndYearAndMonthAndType(request.famName(), request.year(), request.month(), PrayerType.common)
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
        return prayerRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "기도제목을 찾을 수 없습니다."));
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
        return map;
    }
}
