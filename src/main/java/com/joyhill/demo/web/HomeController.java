package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.HomeService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    // 홈 화면 일정 조회
    @GetMapping("/schedules")
    public BaseResponse<List<Map<String, Object>>> schedules() {
        return BaseResponse.success(homeService.schedules());
    }

    // 일정 등록
    @PostMapping("/schedules")
    public BaseResponse<Map<String, Object>> createSchedule(@AuthenticationPrincipal AuthUser authUser,
                                                            @RequestBody AuthDtos.ScheduleRequest request) {
        return BaseResponse.success(homeService.createSchedule(authUser, request));
    }

    // 일정 수정
    @PutMapping("/schedules/{id}")
    public BaseResponse<Map<String, Object>> updateSchedule(@AuthenticationPrincipal AuthUser authUser,
                                                            @PathVariable Long id,
                                                            @RequestBody AuthDtos.ScheduleRequest request) {
        return BaseResponse.success(homeService.updateSchedule(authUser, id, request));
    }

    // 일정 삭제
    @DeleteMapping("/schedules/{id}")
    public BaseResponse<Void> deleteSchedule(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long id) {
        homeService.deleteSchedule(authUser, id);
        return BaseResponse.success();
    }
}
