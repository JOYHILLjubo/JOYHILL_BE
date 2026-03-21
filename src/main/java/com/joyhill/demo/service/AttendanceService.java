package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Attendance;
import com.joyhill.demo.domain.Fam;
import com.joyhill.demo.repository.AttendanceRepository;
import com.joyhill.demo.repository.FamMemberRepository;
import com.joyhill.demo.repository.FamRepository;
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
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final FamMemberRepository famMemberRepository;
    private final FamRepository famRepository;
    private final AccessGuard accessGuard;

    public AttendanceService(AttendanceRepository attendanceRepository, FamMemberRepository famMemberRepository,
                             FamRepository famRepository, AccessGuard accessGuard) {
        this.attendanceRepository = attendanceRepository;
        this.famMemberRepository = famMemberRepository;
        this.famRepository = famRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> get(AuthUser authUser, String famName, LocalDate date) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        return attendanceRepository.findByFamNameAndDate(famName, date).stream().map(this::toMap).toList();
    }

    public void save(AuthUser authUser, AuthDtos.AttendanceSaveRequest request) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, request.famName());
        for (var record : request.records()) {
            attendanceRepository.findByFamMemberIdAndDate(record.famMemberId(), request.date())
                    .ifPresentOrElse(attendance -> update(attendance, request.famName(), request.date(), record),
                            () -> {
                                Attendance attendance = new Attendance();
                                update(attendance, request.famName(), request.date(), record);
                                attendanceRepository.save(attendance);
                            });
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(AuthUser authUser, String famName, int year, int month) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByFamNameAndDateBetween(famName, from, to).stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(AuthUser authUser, String scope, String famName, String villageName, String period) {
        LocalDate to = LocalDate.now();
        LocalDate from = switch (period == null ? "1month" : period) {
            case "3month" -> to.minusMonths(3);
            case "6month" -> to.minusMonths(6);
            default -> to.minusMonths(1);
        };
        List<Attendance> rows;
        if ("all".equals(scope)) {
            if (!(authUser.role().name().equals("pastor") || authUser.role().name().equals("admin"))) {
                throw new ApiException(ErrorCode.FORBIDDEN, "전체 통계 권한이 없습니다.");
            }
            rows = attendanceRepository.findByDateBetween(from, to);
        } else if ("village".equals(scope)) {
            accessGuard.requireRoleAtLeast(authUser, com.joyhill.demo.domain.Role.village_leader);
            accessGuard.requireVillageScope(authUser, villageName == null ? authUser.villageName() : villageName);
            List<String> famNames = famRepository.findByVillageName(villageName == null ? authUser.villageName() : villageName).stream()
                    .map(Fam::getName).toList();
            rows = attendanceRepository.findByFamNameInAndDateBetween(famNames, from, to);
        } else {
            String targetFam = famName == null ? authUser.famName() : famName;
            accessGuard.requireFamScope(authUser, targetFam);
            rows = attendanceRepository.findByFamNameAndDateBetween(targetFam, from, to);
        }
        long total = rows.size();
        long worship = rows.stream().filter(Attendance::isWorshipPresent).count();
        long fam = rows.stream().filter(Attendance::isFamPresent).count();
        Map<String, Object> result = new HashMap<>();
        result.put("scope", scope);
        result.put("period", period);
        result.put("totalRecords", total);
        result.put("worshipPresent", worship);
        result.put("famPresent", fam);
        result.put("worshipRate", total == 0 ? 0.0 : (double) worship / total);
        result.put("famRate", total == 0 ? 0.0 : (double) fam / total);
        return result;
    }

    private void update(Attendance attendance, String famName, LocalDate date, AuthDtos.AttendanceRecordRequest record) {
        famMemberRepository.findById(record.famMemberId()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸원을 찾을 수 없습니다."));
        attendance.setFamMemberId(record.famMemberId());
        attendance.setFamName(famName);
        attendance.setDate(date);
        attendance.setWorshipPresent(record.worshipPresent());
        attendance.setFamPresent(record.famPresent());
    }

    private Map<String, Object> toMap(Attendance attendance) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", attendance.getId());
        map.put("famMemberId", attendance.getFamMemberId());
        map.put("famName", attendance.getFamName());
        map.put("date", attendance.getDate());
        map.put("worshipPresent", attendance.isWorshipPresent());
        map.put("famPresent", attendance.isFamPresent());
        return map;
    }
}
