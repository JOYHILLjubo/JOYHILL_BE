package com.joyhill.demo.web.dto;

import com.joyhill.demo.domain.Role;

import java.time.LocalDate;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    // ── Auth ──
    public record LoginRequest(String phone, String password) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
    public record LoginResponse(String accessToken, UserSummary user) {}
    public record TokenResponse(String accessToken) {}

    public record UserSummary(Long id, String name, Role role, String fam, String village,
                              List<String> teams, List<String> teamRoles, String phone,
                              boolean passwordChanged) {}

    // ── User ──
    public record UserCreateRequest(String name, String phone, String birth, Role role,
                                    String famName, String villageName,
                                    List<String> teams, List<String> teamRoles) {}
    public record UserUpdateRequest(String name, String phone, String birth,
                                    String famName, String villageName) {}
    public record RoleUpdateRequest(Role role) {}

    // ── Organization (팸원 = users 기반) ──
    public record FamMemberCreateRequest(String name, String phone, LocalDate birth, Role role, String note) {}
    public record FamMemberUpdateRequest(String name, String phone, LocalDate birth, String note) {}
    public record FamVillageUpdateRequest(String toVillage) {}
    public record VillageCreateRequest(String name, String leaderName) {}
    public record FamCreateRequest(String name, String villageName, String leaderName) {}

    // ── Attendance (userId 기반으로 변경) ──
    public record AttendanceRecordRequest(Long userId, boolean worshipPresent, boolean famPresent) {}
    public record AttendanceSaveRequest(String famName, LocalDate date, List<AttendanceRecordRequest> records) {}

    // ── Notice ──
    public record NoticeRequest(String title, String content, String tag, String teamTag,
                                boolean pinned, LocalDate deadline, String fileUrl) {}

    // ── Prayer ──
    public record PrayerRequest(String famName, String content, int year, int month, Integer week) {}
    public record CommonPrayerRequest(String famName, String content, int year, int month) {}

    // ── Sermon ──
    public record SermonRequest(String title, String verse, String preacher, String youtubeUrl,
                                String summary, LocalDate sermonDate) {}

    // ── Newcomer ──
    public record NewcomerRequest(String name, String phone, LocalDate birth,
                                  LocalDate registeredAt, String note) {}
    public record NewcomerFamAssignRequest(String famName) {}

    // ── Team ──
    public record TeamMemberAddRequest(Long userId, boolean leader) {}
    public record TeamIntroRequest(String intro) {}

    // ── Schedule ──
    public record ScheduleRequest(LocalDate date, String content, boolean showDDay) {}
}
