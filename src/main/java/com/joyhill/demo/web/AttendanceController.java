package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.AttendanceService;
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

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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

    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String scope,
            @RequestParam(required = false) String famName,
            @RequestParam(required = false) String villageName,
            @RequestParam(required = false) Integer year) {
        return BaseResponse.success(attendanceService.stats(authUser, scope, famName, villageName, year));
    }
}
