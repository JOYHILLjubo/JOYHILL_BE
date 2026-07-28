package com.joyhill.demo.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.Attendance;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.AttendanceRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 청년부 회원 정보를 구글 스프레드시트로 내보내는 백업/참고용 동기화.
 * 통계 컬럼은 AttendanceService.stats()의 정확한 분모(일요일 수 기준)와는 다르게,
 * 단순 "지금까지 기록된 출석 행 수" 기준의 근사치임 — 공식 통계 화면 용도가 아니라 참고용이라 이 정도 단순화로 충분함.
 */
@Service
public class GoogleSheetsSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsSyncService.class);

    private static final List<Object> HEADER = List.of(
            "이름", "역할", "마을", "팸", "전화번호", "생년월일",
            "예배출석(참석)", "예배출석(기록수)", "팸모임출석(참석)", "팸모임출석(기록수)",
            "예배출석률", "팸모임출석률"
    );

    private final Sheets sheetsClient; // null이면 동기화 비활성화 상태
    private final String spreadsheetId;
    private final String tabName;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessGuard accessGuard;

    public GoogleSheetsSyncService(@Autowired(required = false) Sheets sheetsClient,
                                    @Value("${google.sheets.spreadsheet-id:}") String spreadsheetId,
                                    @Value("${google.sheets.tab-name:회원백업}") String tabName,
                                    UserRepository userRepository,
                                    AttendanceRepository attendanceRepository,
                                    AccessGuard accessGuard) {
        this.sheetsClient = sheetsClient;
        this.spreadsheetId = spreadsheetId;
        this.tabName = tabName;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.accessGuard = accessGuard;
    }

    // 교역자/관리자 수동 트리거
    public void syncNow(AuthUser authUser) {
        accessGuard.requirePastorOrAdmin(authUser);
        if (sheetsClient == null || spreadsheetId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "구글시트 연동이 설정되지 않았습니다. 서버에 서비스 계정 키와 스프레드시트 ID를 먼저 설정해주세요.");
        }
        sync();
    }

    // 매일 새벽 3시 자동 동기화 — 설정 전에는 조용히 스킵
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledSync() {
        if (sheetsClient == null || spreadsheetId.isBlank()) {
            return;
        }
        try {
            sync();
        } catch (Exception e) {
            log.error("구글시트 자동 동기화 실패", e);
        }
    }

    @Transactional(readOnly = true)
    void sync() {
        List<User> users = userRepository.findAll();
        Map<Long, List<Attendance>> attendanceByUser = attendanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        List<List<Object>> rows = new ArrayList<>();
        rows.add(HEADER);

        for (User u : users) {
            List<Attendance> records = attendanceByUser.getOrDefault(u.getId(), List.of());
            long worshipAttended = records.stream().filter(Attendance::isWorshipPresent).count() + u.getLegacyWorshipAttended();
            long worshipTotal = records.size() + u.getLegacyWorshipTotal();
            long famAttended = records.stream().filter(Attendance::isFamPresent).count() + u.getLegacyFamAttended();
            long famTotal = records.size() + u.getLegacyFamTotal();

            rows.add(List.of(
                    u.getName(),
                    u.getRole().name(),
                    orEmpty(u.getVillageName()),
                    orEmpty(u.getFamName()),
                    orEmpty(PhoneUtils.format(u.getPhone())),
                    u.getBirth(),
                    worshipAttended, worshipTotal, famAttended, famTotal,
                    percentString(worshipAttended, worshipTotal), percentString(famAttended, famTotal)
            ));
        }

        try {
            sheetsClient.spreadsheets().values()
                    .clear(spreadsheetId, tabName, new ClearValuesRequest())
                    .execute();
            sheetsClient.spreadsheets().values()
                    .update(spreadsheetId, tabName + "!A1", new ValueRange().setValues(rows))
                    .setValueInputOption("RAW")
                    .execute();
            log.info("구글시트 동기화 완료: {}명", users.size());
        } catch (Exception e) {
            throw new RuntimeException("구글시트 동기화 실패", e);
        }
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String percentString(long attended, long total) {
        if (total <= 0) return "-";
        return Math.round(attended * 100.0 / total) + "%";
    }
}
