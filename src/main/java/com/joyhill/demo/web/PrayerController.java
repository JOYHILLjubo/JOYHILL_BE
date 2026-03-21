package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.PrayerService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prayers")
public class PrayerController {

    private final PrayerService prayerService;

    public PrayerController(PrayerService prayerService) {
        this.prayerService = prayerService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal AuthUser authUser,
                                                        @RequestParam(required = false) String famName,
                                                        @RequestParam int year,
                                                        @RequestParam int month,
                                                        @RequestParam(required = false) Integer week) {
        return BaseResponse.success(prayerService.list(authUser, famName, year, month, week));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody AuthDtos.PrayerRequest request) {
        return BaseResponse.success(prayerService.createPersonal(authUser, request));
    }

    @PutMapping("/{id}")
    public BaseResponse<Map<String, Object>> update(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                    @RequestBody AuthDtos.PrayerRequest request) {
        return BaseResponse.success(prayerService.updatePersonal(authUser, id, request));
    }

    @PostMapping("/common")
    public BaseResponse<Map<String, Object>> upsertCommon(@AuthenticationPrincipal AuthUser authUser,
                                                          @RequestBody AuthDtos.CommonPrayerRequest request) {
        return BaseResponse.success(prayerService.upsertCommon(authUser, request));
    }
}
