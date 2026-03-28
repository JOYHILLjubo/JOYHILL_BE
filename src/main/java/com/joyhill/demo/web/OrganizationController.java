package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.OrganizationService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/org/structure")
    public BaseResponse<Map<String, Object>> structure() {
        return BaseResponse.success(organizationService.structure());
    }

    // ── 마을 ──
    @GetMapping("/villages")
    public BaseResponse<List<Map<String, Object>>> villages() {
        return BaseResponse.success(organizationService.villages());
    }

    @PostMapping("/villages")
    public BaseResponse<Map<String, Object>> createVillage(@AuthenticationPrincipal AuthUser authUser,
                                                           @RequestBody AuthDtos.VillageCreateRequest request) {
        return BaseResponse.success(organizationService.createVillage(authUser, request));
    }

    @DeleteMapping("/villages/{villageName}")
    public BaseResponse<Void> deleteVillage(@AuthenticationPrincipal AuthUser authUser,
                                            @PathVariable String villageName) {
        organizationService.deleteVillage(authUser, villageName);
        return BaseResponse.success();
    }

    // ── 팸 ──
    @GetMapping("/fams")
    public BaseResponse<List<Map<String, Object>>> fams() {
        return BaseResponse.success(organizationService.fams());
    }

    @PostMapping("/fams")
    public BaseResponse<Map<String, Object>> createFam(@AuthenticationPrincipal AuthUser authUser,
                                                       @RequestBody AuthDtos.FamCreateRequest request) {
        return BaseResponse.success(organizationService.createFam(authUser, request));
    }

    @PatchMapping("/fams/{famName}/village")
    public BaseResponse<Void> moveFamVillage(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable String famName,
                                             @RequestBody AuthDtos.FamVillageUpdateRequest request) {
        organizationService.moveFamVillage(authUser, famName, request);
        return BaseResponse.success();
    }

    @DeleteMapping("/fams/{famName}")
    public BaseResponse<Void> deleteFam(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable String famName) {
        organizationService.deleteFam(authUser, famName);
        return BaseResponse.success();
    }

    // ── 팸원 ──
    @GetMapping("/fams/{famName}/members")
    public BaseResponse<List<Map<String, Object>>> members(@AuthenticationPrincipal AuthUser authUser,
                                                           @PathVariable String famName,
                                                           @RequestParam(defaultValue = "1month") String period) {
        return BaseResponse.success(organizationService.famMembers(authUser, famName, period));
    }

    @PostMapping("/fams/{famName}/members")
    public BaseResponse<Map<String, Object>> addMember(@AuthenticationPrincipal AuthUser authUser,
                                                       @PathVariable String famName,
                                                       @RequestBody AuthDtos.FamMemberCreateRequest request) {
        return BaseResponse.success(organizationService.addFamMember(authUser, famName, request));
    }

    @PutMapping("/fam-members/{id}")
    public BaseResponse<Map<String, Object>> updateMember(@AuthenticationPrincipal AuthUser authUser,
                                                          @PathVariable Long id,
                                                          @RequestBody AuthDtos.FamMemberUpdateRequest request) {
        return BaseResponse.success(organizationService.updateFamMember(authUser, id, request));
    }

    @PatchMapping("/fam-members/{id}/role")
    public BaseResponse<Void> updateMemberRole(@AuthenticationPrincipal AuthUser authUser,
                                               @PathVariable Long id,
                                               @RequestBody AuthDtos.RoleUpdateRequest request) {
        organizationService.updateFamMemberRole(authUser, id, request);
        return BaseResponse.success();
    }

    @DeleteMapping("/fam-members/{id}")
    public BaseResponse<Void> deleteMember(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long id) {
        organizationService.deleteFamMember(authUser, id);
        return BaseResponse.success();
    }
}
