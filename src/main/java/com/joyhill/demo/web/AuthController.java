package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import com.joyhill.demo.web.dto.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public BaseResponse<AuthDtos.LoginResponse> login(@RequestBody AuthDtos.LoginRequest request,
                                                      HttpServletResponse response) {
        AuthDtos.LoginResponse login = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.refreshCookie(
                authService.getStoredRefreshToken(login.user().id())
        ).toString());
        return BaseResponse.success(login);
    }

    @PostMapping("/refresh")
    public BaseResponse<AuthDtos.TokenResponse> refresh(HttpServletRequest request) {
        return BaseResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public BaseResponse<Void> logout(@AuthenticationPrincipal AuthUser authUser,
                                     HttpServletResponse response) {
        authService.logout(authUser);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.deleteRefreshCookie().toString());
        return BaseResponse.success();
    }

    @PatchMapping("/change-password")
    public BaseResponse<Void> changePassword(@AuthenticationPrincipal AuthUser authUser,
                                             @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(authUser, request);
        return BaseResponse.success();
    }
}
