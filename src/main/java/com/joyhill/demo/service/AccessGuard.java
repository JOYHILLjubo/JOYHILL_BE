package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.security.AuthUser;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private final TeamRoleRepository teamRoleRepository;
    private final FamRepository famRepository;

    public AccessGuard(TeamRoleRepository teamRoleRepository, FamRepository famRepository) {
        this.teamRoleRepository = teamRoleRepository;
        this.famRepository = famRepository;
    }

    public void requireRoleAtLeast(AuthUser user, Role role) {
        if (!user.role().atLeast(role)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    public void requireLeader(AuthUser user) {
        requireRoleAtLeast(user, Role.leader);
    }

    public void requirePastorOrAdmin(AuthUser user) {
        if (!(user.role() == Role.pastor || user.role() == Role.admin)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    public void requireAdmin(AuthUser user) {
        if (user.role() != Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    public void requireNoticeWriter(AuthUser user) {
        if (user.role().atLeast(Role.leader) || teamRoleRepository.existsByUserIdAndLeaderTrue(user.userId())) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "공지 작성 권한이 없습니다.");
    }

    public void requireNewcomerManager(AuthUser user) {
        if (user.role().atLeast(Role.leader) || teamRoleRepository.existsByUserIdAndLeaderTrue(user.userId())) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "새가족 관리 권한이 없습니다.");
    }

    public void requireFamScope(AuthUser user, String famName) {
        if (user.role() == Role.admin || user.role() == Role.pastor) {
            return;
        }
        if (user.role() == Role.leader && famName.equals(user.famName())) {
            return;
        }
        if (user.role() == Role.village_leader) {
            var fam = famRepository.findByName(famName).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
            if (fam.getVillageName().equals(user.villageName())) {
                return;
            }
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "접근 범위를 벗어났습니다.");
    }

    public void requireVillageScope(AuthUser user, String villageName) {
        if (user.role() == Role.admin || user.role() == Role.pastor) {
            return;
        }
        if (user.role() == Role.village_leader && villageName.equals(user.villageName())) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "접근 범위를 벗어났습니다.");
    }
}
