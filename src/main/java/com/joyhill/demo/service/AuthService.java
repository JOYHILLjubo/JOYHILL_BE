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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRoleRepository teamRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, TeamRoleRepository teamRoleRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.teamRoleRepository = teamRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String normalizedPhone = PhoneUtils.normalize(request.phone());
        User user = userRepository.findByPhoneNormalized(normalizedPhone)
                .or(() -> userRepository.findByPhone(request.phone()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "전화번호 또는 비밀번호가 일치하지 않습니다."));

        if (!user.isPasswordChanged()) {
            // 최초 로그인: 입력값과 birth(생년월일) 직접 비교
            if (!request.password().equals(user.getBirth())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "전화번호 또는 비밀번호가 일치하지 않습니다.");
            }
        } else {
            // 이후 로그인: password 컬럼과 BCrypt 비교
            if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "전화번호 또는 비밀번호가 일치하지 않습니다.");
            }
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());
        user.setRefreshToken(hashToken(refreshToken));
        return new AuthDtos.LoginResponse(accessToken, refreshToken, toSummary(user));
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
        if (!hashToken(refreshToken).equals(user.getRefreshToken())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "다시 로그인해주세요.");
        }
        // 슬라이딩 만료: 갱신할 때마다 refresh token도 새로 발급 → 꾸준히 쓰는 한 재로그인 없음
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());
        user.setRefreshToken(hashToken(newRefreshToken));
        return new AuthDtos.TokenResponse(
                jwtTokenProvider.generateAccessToken(user.getId(), user.getRole()),
                newRefreshToken
        );
    }

    public void logout(AuthUser authUser) {
        getUser(authUser.userId()).setRefreshToken(null);
    }

    public void changePassword(AuthUser authUser, AuthDtos.ChangePasswordRequest request) {
        User user = getUser(authUser.userId());

        // 현재 비밀번호 확인: 최초 변경 전이면 birth, 이후면 password(BCrypt)
        if (!user.isPasswordChanged()) {
            if (!request.currentPassword().equals(user.getBirth())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호(생년월일)가 일치하지 않습니다.");
            }
        } else {
            if (user.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 일치하지 않습니다.");
            }
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChanged(true);
    }

    public ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(true).path("/")
                .maxAge(jwtProperties.getRefreshExpiration() / 1000)
                .secure(true)
                .sameSite("Lax").build();
    }

    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true).path("/").maxAge(0).sameSite("Lax").build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtProperties.getRefreshCookieName().equals(cookie.getName())) return cookie.getValue();
            }
        }
        // iOS PWA 등 쿠키가 유지되지 않는 환경을 위한 폴백 (FE가 localStorage에 보관 후 헤더로 전달)
        String header = request.getHeader("X-Refresh-Token");
        if (header != null && !header.isBlank()) return header;
        throw new ApiException(ErrorCode.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public AuthDtos.UserSummary toSummary(User user) {
        var teamRoleEntities = teamRoleRepository.findByUserId(user.getId());
        List<String> teams = teamRoleEntities.stream().map(tr -> tr.getTeamName()).toList();
        List<String> teamRoles = teamRoleEntities.stream()
                .filter(tr -> tr.isLeader()).map(tr -> tr.getTeamName()).toList();
        return new AuthDtos.UserSummary(
                user.getId(), user.getName(), user.getRole(),
                user.getFamName(), user.getVillageName(),
                teams, teamRoles, PhoneUtils.format(user.getPhone()), user.getBirth(),
                user.isPasswordChanged(), user.getAvatarKey(), user.getAvatarPhotoUrl()
        );
    }
}
