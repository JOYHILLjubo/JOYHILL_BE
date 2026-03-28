package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.TeamService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // 팀 목록 (소개 + 팀원 수)
    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list() {
        return BaseResponse.success(teamService.list());
    }

    // 팀원 목록
    @GetMapping("/{teamName}/members")
    public BaseResponse<List<Map<String, Object>>> members(@PathVariable String teamName) {
        return BaseResponse.success(teamService.members(teamName));
    }

    // 팀원 추가
    @PostMapping("/{teamName}/members")
    public BaseResponse<Map<String, Object>> addMember(@AuthenticationPrincipal AuthUser authUser,
                                                       @PathVariable String teamName,
                                                       @RequestBody AuthDtos.TeamMemberAddRequest request) {
        return BaseResponse.success(teamService.addMember(authUser, teamName, request));
    }

    // 팀원 제거
    @DeleteMapping("/{teamName}/members/{userId}")
    public BaseResponse<Void> removeMember(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable String teamName,
                                           @PathVariable Long userId) {
        teamService.removeMember(authUser, teamName, userId);
        return BaseResponse.success();
    }

    // 팀 소개 수정
    @PatchMapping("/{teamName}/intro")
    public BaseResponse<Map<String, Object>> updateIntro(@AuthenticationPrincipal AuthUser authUser,
                                                         @PathVariable String teamName,
                                                         @RequestBody AuthDtos.TeamIntroRequest request) {
        return BaseResponse.success(teamService.updateIntro(authUser, teamName, request));
    }
}
