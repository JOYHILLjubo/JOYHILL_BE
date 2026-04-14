package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.CommunityPrayerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community-prayers")
public class CommunityPrayerController {

    private final CommunityPrayerService communityPrayerService;

    public CommunityPrayerController(CommunityPrayerService communityPrayerService) {
        this.communityPrayerService = communityPrayerService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list() {
        return BaseResponse.success(communityPrayerService.list());
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody Map<String, String> body) {
        return BaseResponse.success(communityPrayerService.create(authUser, body.get("content")));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id) {
        communityPrayerService.delete(authUser, id);
        return BaseResponse.success();
    }
}
