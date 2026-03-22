package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.User;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.security.JwtProperties;
import com.joyhill.demo.security.JwtTokenProvider;
import com.joyhill.demo.web.dto.AuthDtos;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, TeamRoleRepository teamRoleRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String normalizedPhone = PhoneUtils.normalize(request.phone());
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "전화번호 또는 비밀번호가 일치하지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "전화번호 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());
        user.setRefreshToken(refreshToken);
        return new AuthDtos.LoginResponse(accessToken, toSummary(user));
    }

    public String getStoredRefreshToken(Long userId) {
        return getUser(userId).getRefreshToken();
    }

    public AuthDtos.TokenResponse refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
        var claims = jwtTokenProvider.parse(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "다시 로그인해주세요."));
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "다시 로그인해주세요.");
        }
        return new AuthDtos.TokenResponse(jwtTokenProvider.generateAccessToken(user.getId(), user.getRole()));
    }

    public void logout(AuthUser authUser) {
        User user = getUser(authUser.userId());
        user.setRefreshToken(null);
    }

    public void changePassword(AuthUser authUser, AuthDtos.ChangePasswordRequest request) {
        User user = getUser(authUser.userId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    public ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getRefreshExpiration() / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public AuthDtos.UserSummary toSummary(User user) {
        var teamRoleEntities = teamRoleRepository.findByUserId(user.getId());

        // teams: 소속된 모든 팀 이름
        List<String> teams = teamRoleEntities.stream()
                .map(tr -> tr.getTeamName())
                .toList();

        // teamRoles: 팀장인 팀 이름만 (프론트에서 user.teamRoles.includes("찬양팀") 형태로 사용)
        List<String> teamRoles = teamRoleEntities.stream()
                .filter(tr -> tr.isLeader())
                .map(tr -> tr.getTeamName())
                .toList();

        return new AuthDtos.UserSummary(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getFamName(),
                user.getVillageName(),
                teams,
                teamRoles,
                user.getPhone()
        );
    }
}
