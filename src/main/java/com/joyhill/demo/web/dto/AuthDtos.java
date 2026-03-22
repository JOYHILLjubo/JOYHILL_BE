package com.joyhill.demo.web.dto;

import com.joyhill.demo.domain.Role;

import java.time.LocalDate;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(String phone, String password) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    public record LoginResponse(String accessToken, UserSummary user) {
    }

    // teamRoles: 팀장인 팀 이름 목록 (프론트 user.teamRoles.includes("찬양팀") 형태로 사용)
    // teams: 소속된 모든 팀 이름 목록
    public record UserSummary(Long id, String name, Role role, String fam, String village,
                              List<String> teams, List<String> teamRoles, String phone) {
    }

    public record TokenResponse(String accessToken) {
    }

    public record FamMemberCreateRequest(String name, String phone, LocalDate birth, Role role, String note) {
    }

    public record FamMemberUpdateRequest(String name, String phone, LocalDate birth, String note) {
    }

    public record RoleUpdateRequest(Role role) {
    }

    public record FamVillageUpdateRequest(String toVillage) {
    }

    public record UserCreateRequest(String name, String phone, String birth, Role role, String famName, String villageName,
                                    List<String> teams, List<String> teamRoles) {
    }

    public record UserUpdateRequest(String name, String phone, String birth, String famName, String villageName) {
    }

    public record AttendanceRecordRequest(Long famMemberId, boolean worshipPresent, boolean famPresent) {
    }

    public record AttendanceSaveRequest(String famName, LocalDate date, List<AttendanceRecordRequest> records) {
    }

    public record NoticeRequest(String title, String content, String tag, String teamTag, boolean pinned, LocalDate deadline,
                                String fileUrl) {
    }

    public record PrayerRequest(String famName, String content, int year, int month, Integer week) {
    }

    public record CommonPrayerRequest(String famName, String content, int year, int month) {
    }

    public record SermonRequest(String title, String verse, String preacher, String youtubeUrl, String summary,
                                LocalDate sermonDate) {
    }

    public record NewcomerRequest(String name, String phone, LocalDate birth, LocalDate registeredAt, String note) {
    }

    public record NewcomerFamAssignRequest(String famName) {
    }
}
