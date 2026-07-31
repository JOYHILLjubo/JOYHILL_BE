package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.AttendanceService;
import com.joyhill.demo.service.GoogleSheetsSyncService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final GoogleSheetsSyncService googleSheetsSyncService;

    public AttendanceController(AttendanceService attendanceService, GoogleSheetsSyncService googleSheetsSyncService) {
        this.attendanceService = attendanceService;
        this.googleSheetsSyncService = googleSheetsSyncService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> get(@AuthenticationPrincipal AuthUser authUser,
                                                       @RequestParam String famName,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return BaseResponse.success(attendanceService.get(authUser, famName, date));
    }

    @PostMapping
    public BaseResponse<Void> save(@AuthenticationPrincipal AuthUser authUser,
                                   @RequestBody AuthDtos.AttendanceSaveRequest request) {
        attendanceService.save(authUser, request);
        return BaseResponse.success();
    }

    @GetMapping("/history")
    public BaseResponse<List<Map<String, Object>>> history(@AuthenticationPrincipal AuthUser authUser,
                                                           @RequestParam String famName,
                                                           @RequestParam int year,
                                                           @RequestParam int month) {
        return BaseResponse.success(attendanceService.history(authUser, famName, year, month));
    }

    @GetMapping("/check-status")
    public BaseResponse<List<Map<String, Object>>> checkStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return BaseResponse.success(attendanceService.checkStatus(authUser, date));
    }

    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String scope,
            @RequestParam(required = false) String famName,
            @RequestParam(required = false) String villageName,
            @RequestParam(required = false) Integer year) {
        return BaseResponse.success(attendanceService.stats(authUser, scope, famName, villageName, year));
    }

    // 관리자 수동 트리거: 출석 통계를 구글시트로 백업/동기화 (출석 통계 페이지)
    @PostMapping("/sync-sheet")
    public BaseResponse<Void> syncSheet(@AuthenticationPrincipal AuthUser authUser) {
        googleSheetsSyncService.syncAttendanceNow(authUser);
        return BaseResponse.success();
    }
}
