package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Schedule;
import com.joyhill.demo.repository.ScheduleRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HomeService {

    private final ScheduleRepository scheduleRepository;
    private final AccessGuard accessGuard;

    public HomeService(ScheduleRepository scheduleRepository, AccessGuard accessGuard) {
        this.scheduleRepository = scheduleRepository;
        this.accessGuard = accessGuard;
    }

    // 홈 화면 일정 (오늘 이후 일정)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> schedules() {
        return scheduleRepository.findByDateGreaterThanEqualOrderByDateAsc(LocalDate.now())
                .stream().map(this::toMap).toList();
    }

    // ── 일정 CRUD ──
    public Map<String, Object> createSchedule(AuthUser authUser, AuthDtos.ScheduleRequest request) {
        accessGuard.requirePastorOrAdmin(authUser);
        Schedule s = new Schedule();
        s.setDate(request.date());
        s.setContent(request.content());
        s.setShowDDay(request.showDDay());
        scheduleRepository.save(s);
        return toMap(s);
    }

    public Map<String, Object> updateSchedule(AuthUser authUser, Long id, AuthDtos.ScheduleRequest request) {
        accessGuard.requirePastorOrAdmin(authUser);
        Schedule s = getSchedule(id);
        s.setDate(request.date());
        s.setContent(request.content());
        s.setShowDDay(request.showDDay());
        return toMap(s);
    }

    public void deleteSchedule(AuthUser authUser, Long id) {
        accessGuard.requirePastorOrAdmin(authUser);
        scheduleRepository.delete(getSchedule(id));
    }

    private Schedule getSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private Map<String, Object> toMap(Schedule s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("date", s.getDate());
        map.put("content", s.getContent());
        map.put("showDDay", s.isShowDDay());
        if (s.isShowDDay()) {
            long days = LocalDate.now().until(s.getDate()).getDays();
            map.put("dDay", days == 0 ? "D-Day" : (days > 0 ? "D-" + days : "D+" + Math.abs(days)));
        }
        return map;
    }
}
