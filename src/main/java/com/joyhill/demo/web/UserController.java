package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.AuthService;
import com.joyhill.demo.service.GoogleSheetsSyncService;
import com.joyhill.demo.service.UserService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final GoogleSheetsSyncService googleSheetsSyncService;

    public UserController(UserService userService, AuthService authService,
                          GoogleSheetsSyncService googleSheetsSyncService) {
        this.userService = userService;
        this.authService = authService;
        this.googleSheetsSyncService = googleSheetsSyncService;
    }

    // 현재 로그인 유저 정보
    @GetMapping("/me")
    public BaseResponse<AuthDtos.UserSummary> me(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(userService.me(authUser));
    }

    // 이달의 생일 - 청년부 전체 공개(권한 제한 없음)
    @GetMapping("/birthdays")
    public BaseResponse<List<Map<String, Object>>> birthdays() {
        return BaseResponse.success(userService.birthdaysThisMonth());
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> users(@AuthenticationPrincipal AuthUser authUser,
                                                         @RequestParam(required = false) Role role,
                                                         @RequestParam(required = false) String search) {
        return BaseResponse.success(userService.users(authUser, role, search));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody AuthDtos.UserCreateRequest request) {
        return BaseResponse.success(userService.create(authUser, request));
    }

    @PutMapping("/{id}")
    public BaseResponse<Map<String, Object>> update(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long id,
                                                    @RequestBody AuthDtos.UserUpdateRequest request) {
        return BaseResponse.success(userService.update(authUser, id, request));
    }

    @PatchMapping("/{id}/role")
    public BaseResponse<Void> changeRole(@AuthenticationPrincipal AuthUser authUser,
                                         @PathVariable Long id,
                                         @RequestBody AuthDtos.RoleUpdateRequest request) {
        userService.changeRole(authUser, id, request);
        return BaseResponse.success();
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        userService.delete(authUser, id);
        return BaseResponse.success();
    }

    @PatchMapping("/{id}/reset-password")
    public BaseResponse<Void> resetPassword(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        userService.resetPassword(authUser, id);
        return BaseResponse.success();
    }

    @PatchMapping("/me/avatar")
    public BaseResponse<Void> updateAvatar(@AuthenticationPrincipal AuthUser authUser,
                                           @RequestBody AuthDtos.AvatarUpdateRequest request) {
        userService.updateAvatar(authUser, request.avatarKey());
        return BaseResponse.success();
    }

    // 관리자 수동 트리거: 회원 정보를 구글시트로 백업/동기화 (청년부 전체 관리 페이지)
    @PostMapping("/sync-sheet")
    public BaseResponse<Void> syncSheet(@AuthenticationPrincipal AuthUser authUser) {
        googleSheetsSyncService.syncMembersNow(authUser);
        return BaseResponse.success();
    }
}
