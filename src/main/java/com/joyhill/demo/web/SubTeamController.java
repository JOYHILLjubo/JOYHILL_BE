package com.joyhill.demo.web;
import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.SubTeamService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/teams/{teamName}/sub-teams")
public class SubTeamController {
    private final SubTeamService subTeamService;
    public SubTeamController(SubTeamService subTeamService) {
        this.subTeamService = subTeamService;
    }
    @GetMapping
    public BaseResponse<List<Map<String, Object>>> listSubTeams(@PathVariable String teamName) {
        return BaseResponse.success(subTeamService.listSubTeams(teamName));
    }
    @PostMapping
    public BaseResponse<Map<String, Object>> createSubTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamName,
            @RequestBody AuthDtos.SubTeamCreateRequest request) {
        return BaseResponse.success(subTeamService.createSubTeam(authUser, teamName, request));
    }
    @DeleteMapping("/{subTeamName}")
    public BaseResponse<Void> deleteSubTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamName,
            @PathVariable String subTeamName) {
        subTeamService.deleteSubTeam(authUser, teamName, subTeamName);
        return BaseResponse.success();
    }
    @GetMapping("/{subTeamName}/members")
    public BaseResponse<List<Map<String, Object>>> members(
            @PathVariable String teamName,
            @PathVariable String subTeamName) {
        return BaseResponse.success(subTeamService.members(teamName, subTeamName));
    }
    @PostMapping("/{subTeamName}/members")
    public BaseResponse<Map<String, Object>> addMember(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamName,
            @PathVariable String subTeamName,
            @RequestBody AuthDtos.SubTeamMemberAddRequest request) {
        return BaseResponse.success(subTeamService.addMember(authUser, teamName, subTeamName, request));
    }
    @DeleteMapping("/{subTeamName}/members/{userId}")
    public BaseResponse<Void> removeMember(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamName,
            @PathVariable String subTeamName,
            @PathVariable Long userId) {
        subTeamService.removeMember(authUser, teamName, subTeamName, userId);
        return BaseResponse.success();
    }
    @PutMapping("/{subTeamName}/leader")
    public BaseResponse<Map<String, Object>> setLeader(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String teamName,
            @PathVariable String subTeamName,
            @RequestBody AuthDtos.SubTeamLeaderRequest request) {
        return BaseResponse.success(subTeamService.setLeader(authUser, teamName, subTeamName, request));
    }
}
