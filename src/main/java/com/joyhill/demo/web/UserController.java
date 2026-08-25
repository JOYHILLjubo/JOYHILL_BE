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
import org.springframework.web.multipart.MultipartFile;

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

    // 본인 정보 수정(이름/전화번호/생년월일). 관리자용 PUT /{id}와 달리 권한 검사 대신
    // 대상이 항상 본인으로 고정되고 바꿀 수 있는 항목도 제한된다.
    @PatchMapping("/me")
    public BaseResponse<Map<String, Object>> updateMe(@AuthenticationPrincipal AuthUser authUser,
                                                      @RequestBody AuthDtos.MeUpdateRequest request) {
        return BaseResponse.success(userService.updateMe(authUser, request));
    }

    @PatchMapping("/me/avatar")
    public BaseResponse<Void> updateAvatar(@AuthenticationPrincipal AuthUser authUser,
                                           @RequestBody AuthDtos.AvatarUpdateRequest request) {
        userService.updateAvatar(authUser, request.avatarKey(), request.avatarPhotoUrl());
        return BaseResponse.success();
    }

    // 내 사진 업로드 — S3에 올리고 URL만 반환(아직 저장 안 함, PATCH /me/avatar로 적용해야 실제 반영됨)
    @PostMapping("/me/avatar-photo")
    public BaseResponse<Map<String, Object>> uploadAvatarPhoto(@RequestParam("photo") MultipartFile photo) {
        String url = userService.uploadAvatarPhoto(photo);
        return BaseResponse.success(Map.of("avatarPhotoUrl", url));
    }

    // 관리자 수동 트리거: 회원 정보를 구글시트로 백업/동기화 (청년부 전체 관리 페이지)
    // 응답에 갱신된 마지막 백업 시각을 실어 보내서 프론트가 따로 다시 조회하지 않아도 되게 함
    @PostMapping("/sync-sheet")
    public BaseResponse<Map<String, Object>> syncSheet(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(googleSheetsSyncService.syncMembersNow(authUser));
    }

    // 마지막 백업 시각 조회
    @GetMapping("/sync-sheet")
    public BaseResponse<Map<String, Object>> syncSheetStatus(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(
                googleSheetsSyncService.status(authUser, GoogleSheetsSyncService.TYPE_MEMBERS));
    }
}
