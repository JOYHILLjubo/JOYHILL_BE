package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Attendance;
import com.joyhill.demo.domain.Fam;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.AttendanceRepository;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final FamRepository famRepository;
    private final AccessGuard accessGuard;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             UserRepository userRepository,
                             FamRepository famRepository,
                             AccessGuard accessGuard) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.famRepository = famRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> get(AuthUser authUser, String famName, LocalDate date) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, famName);
        return attendanceRepository.findByFamNameAndDate(famName, date).stream()
                .map(this::toMap).toList();
    }

    public void save(AuthUser authUser, AuthDtos.AttendanceSaveRequest request) {
        accessGuard.requireLeader(authUser);
        accessGuard.requireFamScope(authUser, request.famName());
        for (var record : request.records()) {
            attendanceRepository.findByUserIdAndDate(record.userId(), request.date())
                    .ifPresentOrElse(
                            attendance -> update(attendance, request.famName(), request.date(), record),
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
        return attendanceRepository.findByFamNameAndDateBetween(famName, from, to).stream()
                .map(this::toMap).toList();
    }

    /**
     * 출석 통계
     * @param year 연도 지정 시 해당 연도 1/1 ~ 12/31 (현재 연도면 오늘까지)
     *             null 이면 전체 데이터 기준
     */
    @Transactional(readOnly = true)
    public Map<String, Object> stats(AuthUser authUser, String scope, String famName,
                                     String villageName, Integer year) {
        LocalDate to;
        LocalDate from;

        if (year != null) {
            int currentYear = LocalDate.now().getYear();
            from = LocalDate.of(year, 1, 1);
            // 현재 연도면 오늘까지, 과거 연도면 12/31까지
            to = (year == currentYear) ? LocalDate.now() : LocalDate.of(year, 12, 31);
        } else {
            // year 미지정 시 전체 기간 (충분히 넓게)
            from = LocalDate.of(2000, 1, 1);
            to = LocalDate.now();
        }

        List<Attendance> rows;
        boolean isCurrentYear = (year != null && year == LocalDate.now().getYear());

        if ("all".equals(scope)) {
            if (!(authUser.role() == Role.pastor || authUser.role() == Role.admin)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "전체 통계 권한이 없습니다.");
            }
            rows = attendanceRepository.findByDateBetween(from, to);

        } else if ("village".equals(scope)) {
            accessGuard.requireRoleAtLeast(authUser, Role.village_leader);
            String targetVillage = villageName == null ? authUser.villageName() : villageName;
            accessGuard.requireVillageScope(authUser, targetVillage);
            List<String> famNames = famRepository.findByVillageName(targetVillage).stream()
                    .map(Fam::getName).toList();
            rows = attendanceRepository.findByFamNameInAndDateBetween(famNames, from, to);

        } else {
            String targetFam = famName == null ? authUser.famName() : famName;
            accessGuard.requireFamScope(authUser, targetFam);
            rows = attendanceRepository.findByFamNameAndDateBetween(targetFam, from, to);
        }

        long newWorship = rows.stream().filter(Attendance::isWorshipPresent).count();
        long newOnline  = rows.stream().filter(Attendance::isOnlinePresent).count();
        long newFam     = rows.stream().filter(Attendance::isFamPresent).count();

        // 분모 = 올해 총 일요일 수 × 범위 내 팸원 수
        // (attendance row 수를 분모로 쓰면 팸원 수만큼 곱해져서 % 이상해짐)
        LocalDate sundayBase = isCurrentYear ? LocalDate.of(LocalDate.now().getYear(), 1, 1) : from;
        int sundaysInPeriod = countSundaysInRange(sundayBase, to);

        List<User> scopeUsers;
        if ("all".equals(scope)) {
            scopeUsers = userRepository.findAll().stream()
                    .filter(u -> u.getFamName() != null && !u.getFamName().isBlank())
                    .toList();
        } else if ("village".equals(scope)) {
            String targetVillage = villageName == null ? authUser.villageName() : villageName;
            scopeUsers = userRepository.findAll().stream()
                    .filter(u -> targetVillage.equals(u.getVillageName())
                            && u.getFamName() != null && !u.getFamName().isBlank())
                    .toList();
        } else {
            String targetFam = famName == null ? authUser.famName() : famName;
            scopeUsers = userRepository.findByFamName(targetFam);
        }

        long memberCount = scopeUsers.size();
        long denominator = (long) sundaysInPeriod * memberCount;

        // legacy 합산 (현재 연도에만)
        long legacyWorshipAtt = 0;
        long legacyFamAtt = 0;
        if (isCurrentYear) {
            legacyWorshipAtt = scopeUsers.stream().mapToLong(User::getLegacyWorshipAttended).sum();
            legacyFamAtt     = scopeUsers.stream().mapToLong(User::getLegacyFamAttended).sum();
        }

        long worshipAtt = newWorship + legacyWorshipAtt;
        long famAtt     = newFam     + legacyFamAtt;

        Map<String, Object> result = new HashMap<>();
        result.put("scope", scope);
        result.put("year", year);
        result.put("from", from);
        result.put("to", to);
        result.put("totalRecords", rows.size());
        result.put("worshipPresent", worshipAtt);
        result.put("onlinePresent", newOnline);
        result.put("famPresent", famAtt);
        result.put("worshipRate", denominator == 0 ? 0.0 : Math.round((double) worshipAtt / denominator * 100.0));
        result.put("onlineRate",  denominator == 0 ? 0.0 : Math.round((double) newOnline  / denominator * 100.0));
        result.put("famRate",     denominator == 0 ? 0.0 : Math.round((double) famAtt     / denominator * 100.0));
        return result;
    }

    private void update(Attendance attendance, String famName, LocalDate date,
                        AuthDtos.AttendanceRecordRequest record) {
        userRepository.findById(record.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸원을 찾을 수 없습니다."));
        attendance.setUserId(record.userId());
        attendance.setFamName(famName);
        attendance.setDate(date);
        attendance.setWorshipPresent(record.worshipPresent());
        attendance.setOnlinePresent(record.onlinePresent());
        attendance.setFamPresent(record.famPresent());
    }

    private int countSundaysInRange(LocalDate from, LocalDate to) {
        LocalDate firstSunday = from;
        while (firstSunday.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            firstSunday = firstSunday.plusDays(1);
        }
        if (firstSunday.isAfter(to)) return 0;
        return (int) java.time.temporal.ChronoUnit.WEEKS.between(firstSunday, to) + 1;
    }

    private Map<String, Object> toMap(Attendance attendance) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", attendance.getId());
        map.put("userId", attendance.getUserId());
        map.put("famMemberId", attendance.getUserId()); // 프론트 호환성
        map.put("famName", attendance.getFamName());
        map.put("date", attendance.getDate());
        map.put("worshipPresent", attendance.isWorshipPresent());
        map.put("onlinePresent", attendance.isOnlinePresent());
        map.put("famPresent", attendance.isFamPresent());
        return map;
    }
}
