package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.AuthService;
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

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // 현재 로그인 유저 정보
    @GetMapping("/me")
    public BaseResponse<AuthDtos.UserSummary> me(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(userService.me(authUser));
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
}
