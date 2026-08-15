package com.joyhill.demo.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.Attendance;
import com.joyhill.demo.domain.SheetSyncLog;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.AttendanceRepository;
import com.joyhill.demo.repository.SheetSyncLogRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 청년부 회원 정보 / 출석 통계를 구글 스프레드시트로 내보내는 백업/참고용 동기화.
 * 청년부 전체 관리 페이지(회원 정보)와 출석 통계 페이지(출석 통계)에서 각각 별도로 트리거하고,
 * 시트 탭도 분리되어 있음 — 관리 페이지에서 회원 정보만 바뀐 경우 출석 탭까지 매번 다시 쓸 필요는 없다는 판단.
 * 통계 컬럼은 AttendanceService.stats()의 정확한 분모(일요일 수 기준)와는 다르게,
 * 단순 "지금까지 기록된 출석 행 수" 기준의 근사치임 — 공식 통계 화면 용도가 아니라 참고용이라 이 정도 단순화로 충분함.
 */
@Service
public class GoogleSheetsSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsSyncService.class);

    private static final List<Object> MEMBER_HEADER = List.of(
            "이름", "역할", "마을", "팸", "전화번호", "생년월일"
    );

    private static final List<Object> ATTENDANCE_HEADER = List.of(
            "이름", "마을", "팸",
            "예배출석(참석)", "예배출석(기록수)", "팸모임출석(참석)", "팸모임출석(기록수)",
            "예배출석률", "팸모임출석률"
    );

    private final Sheets sheetsClient; // null이면 동기화 비활성화 상태
    private final String spreadsheetId;
    private final String memberTabName;
    private final String attendanceTabName;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final SheetSyncLogRepository sheetSyncLogRepository;
    private final AccessGuard accessGuard;

    public static final String TYPE_MEMBERS = "members";
    public static final String TYPE_ATTENDANCE = "attendance";

    public GoogleSheetsSyncService(@Autowired(required = false) Sheets sheetsClient,
                                    @Value("${google.sheets.spreadsheet-id:}") String spreadsheetId,
                                    @Value("${google.sheets.tab-name:회원백업}") String memberTabName,
                                    @Value("${google.sheets.attendance-tab-name:출석통계}") String attendanceTabName,
                                    UserRepository userRepository,
                                    AttendanceRepository attendanceRepository,
                                    SheetSyncLogRepository sheetSyncLogRepository,
                                    AccessGuard accessGuard) {
        this.sheetsClient = sheetsClient;
        this.spreadsheetId = spreadsheetId;
        this.memberTabName = memberTabName;
        this.attendanceTabName = attendanceTabName;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.sheetSyncLogRepository = sheetSyncLogRepository;
        this.accessGuard = accessGuard;
    }

    // 교역자/관리자 수동 트리거 — 청년부 전체 관리 페이지: 회원 정보만
    public Map<String, Object> syncMembersNow(AuthUser authUser) {
        accessGuard.requirePastorOrAdmin(authUser);
        requireConfigured();
        syncMembers();
        return status(authUser, TYPE_MEMBERS);
    }

    // 교역자/관리자 수동 트리거 — 출석 통계 페이지: 출석 통계만
    public Map<String, Object> syncAttendanceNow(AuthUser authUser) {
        accessGuard.requirePastorOrAdmin(authUser);
        requireConfigured();
        syncAttendance();
        return status(authUser, TYPE_ATTENDANCE);
    }

    /** 마지막 백업 시각 조회 — 화면의 "마지막 백업: ..." 표시용. 한 번도 안 했으면 syncedAt이 null. */
    @Transactional(readOnly = true)
    public Map<String, Object> status(AuthUser authUser, String syncType) {
        accessGuard.requirePastorOrAdmin(authUser);
        Map<String, Object> map = new HashMap<>();
        map.put("syncType", syncType);
        sheetSyncLogRepository.findBySyncType(syncType).ifPresentOrElse(syncLog -> {
            // LocalDateTime.toString()은 타임존이 없어서 브라우저가 자기 로컬 시간으로 오해한다.
            // 서버 타임존 오프셋을 붙여 보내야 클라이언트에서 정확한 시각으로 표시된다.
            map.put("syncedAt", syncLog.getSyncedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
            map.put("rowCount", syncLog.getRowCount());
        }, () -> {
            map.put("syncedAt", null);
            map.put("rowCount", 0);
        });
        return map;
    }

    private void recordSync(String syncType, int rowCount) {
        SheetSyncLog syncLog = sheetSyncLogRepository.findBySyncType(syncType)
                .orElseGet(() -> new SheetSyncLog(syncType, LocalDateTime.now(), rowCount));
        syncLog.setSyncedAt(LocalDateTime.now());
        syncLog.setRowCount(rowCount);
        sheetSyncLogRepository.save(syncLog);
    }

    private void requireConfigured() {
        if (sheetsClient == null || spreadsheetId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "구글시트 연동이 설정되지 않았습니다. 서버에 서비스 계정 키와 스프레드시트 ID를 먼저 설정해주세요.");
        }
    }

    // 매일 새벽 3시(KST) 자동 동기화 — 설정 전에는 조용히 스킵, 회원/출석 각각 독립적으로 실패 처리.
    // zone을 명시하지 않으면 JVM 기본 타임존을 따르는데, 이 서버는 UTC로 돌고 있어서
    // 실제로는 낮 12시(KST)에 실행되고 있었다. 사람들이 앱을 쓰는 시간대라 새벽으로 되돌림.
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void scheduledSync() {
        if (sheetsClient == null || spreadsheetId.isBlank()) {
            return;
        }
        try {
            syncMembers();
        } catch (Exception e) {
            log.error("구글시트 회원 정보 자동 동기화 실패", e);
        }
        try {
            syncAttendance();
        } catch (Exception e) {
            log.error("구글시트 출석 통계 자동 동기화 실패", e);
        }
    }

    @Transactional
    void syncMembers() {
        List<User> users = userRepository.findAll();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(MEMBER_HEADER);
        for (User u : users) {
            rows.add(List.of(
                    u.getName(),
                    u.getRole().name(),
                    orEmpty(u.getVillageName()),
                    orEmpty(u.getFamName()),
                    orEmpty(PhoneUtils.format(u.getPhone())),
                    u.getBirth()
            ));
        }

        writeRows(memberTabName, rows, "회원 정보", users.size(), TYPE_MEMBERS);
    }

    @Transactional
    void syncAttendance() {
        List<User> users = userRepository.findAll();
        Map<Long, List<Attendance>> attendanceByUser = attendanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        List<List<Object>> rows = new ArrayList<>();
        rows.add(ATTENDANCE_HEADER);

        for (User u : users) {
            List<Attendance> records = attendanceByUser.getOrDefault(u.getId(), List.of());
            long worshipAttended = records.stream().filter(Attendance::isWorshipPresent).count() + u.getLegacyWorshipAttended();
            long worshipTotal = records.size() + u.getLegacyWorshipTotal();
            long famAttended = records.stream().filter(Attendance::isFamPresent).count() + u.getLegacyFamAttended();
            long famTotal = records.size() + u.getLegacyFamTotal();

            rows.add(List.of(
                    u.getName(),
                    orEmpty(u.getVillageName()),
                    orEmpty(u.getFamName()),
                    worshipAttended, worshipTotal, famAttended, famTotal,
                    percentString(worshipAttended, worshipTotal), percentString(famAttended, famTotal)
            ));
        }

        writeRows(attendanceTabName, rows, "출석 통계", users.size(), TYPE_ATTENDANCE);
    }

    private void writeRows(String tabName, List<List<Object>> rows, String label, int userCount, String syncType) {
        try {
            ensureTabExists(tabName);
            sheetsClient.spreadsheets().values()
                    .clear(spreadsheetId, tabName, new ClearValuesRequest())
                    .execute();
            sheetsClient.spreadsheets().values()
                    .update(spreadsheetId, tabName + "!A1", new ValueRange().setValues(rows))
                    .setValueInputOption("RAW")
                    .execute();
            recordSync(syncType, userCount);
            log.info("구글시트 {} 동기화 완료: {}명", label, userCount);
        } catch (Exception e) {
            log.error("구글시트 {} 동기화 실패", label, e);
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "구글시트 동기화에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // values.clear/update는 이미 존재하는 탭만 다룰 수 있어서, 새 탭 이름(예: attendance-tab-name 도입)이면
    // 스프레드시트에 사람이 미리 탭을 만들어두지 않는 한 매번 실패함 — 없으면 여기서 자동 생성.
    private void ensureTabExists(String tabName) throws java.io.IOException {
        boolean exists = sheetsClient.spreadsheets().get(spreadsheetId).execute().getSheets().stream()
                .anyMatch(sheet -> tabName.equals(sheet.getProperties().getTitle()));
        if (exists) return;

        sheetsClient.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(new Request().setAddSheet(
                        new AddSheetRequest().setProperties(new SheetProperties().setTitle(tabName))
                )))).execute();
        log.info("구글시트 탭 '{}'이(가) 없어서 새로 생성함", tabName);
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String percentString(long attended, long total) {
        if (total <= 0) return "-";
        return Math.round(attended * 100.0 / total) + "%";
    }
}
