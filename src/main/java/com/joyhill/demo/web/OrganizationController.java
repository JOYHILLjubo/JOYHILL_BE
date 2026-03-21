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

    @GetMapping("/villages")
    public BaseResponse<List<Map<String, Object>>> villages() {
        return BaseResponse.success(organizationService.villages());
    }

    @DeleteMapping("/villages/{villageName}")
    public BaseResponse<Void> deleteVillage(@AuthenticationPrincipal AuthUser authUser, @PathVariable String villageName) {
        organizationService.deleteVillage(authUser, villageName);
        return BaseResponse.success();
    }

    @GetMapping("/fams")
    public BaseResponse<List<Map<String, Object>>> fams() {
        return BaseResponse.success(organizationService.fams());
    }

    @PatchMapping("/fams/{famName}/village")
    public BaseResponse<Void> moveFamVillage(@AuthenticationPrincipal AuthUser authUser, @PathVariable String famName,
                                             @RequestBody AuthDtos.FamVillageUpdateRequest request) {
        organizationService.moveFamVillage(authUser, famName, request);
        return BaseResponse.success();
    }

    @DeleteMapping("/fams/{famName}")
    public BaseResponse<Void> deleteFam(@AuthenticationPrincipal AuthUser authUser, @PathVariable String famName) {
        organizationService.deleteFam(authUser, famName);
        return BaseResponse.success();
    }

    @GetMapping("/fams/{famName}/members")
    public BaseResponse<List<Map<String, Object>>> members(@AuthenticationPrincipal AuthUser authUser, @PathVariable String famName) {
        return BaseResponse.success(organizationService.famMembers(authUser, famName));
    }

    @PostMapping("/fams/{famName}/members")
    public BaseResponse<Map<String, Object>> addMember(@AuthenticationPrincipal AuthUser authUser, @PathVariable String famName,
                                                       @RequestBody AuthDtos.FamMemberCreateRequest request) {
        return BaseResponse.success(organizationService.addFamMember(authUser, famName, request));
    }

    @PutMapping("/fam-members/{id}")
    public BaseResponse<Map<String, Object>> updateMember(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                          @RequestBody AuthDtos.FamMemberUpdateRequest request) {
        return BaseResponse.success(organizationService.updateFamMember(authUser, id, request));
    }

    @PatchMapping("/fam-members/{id}/role")
    public BaseResponse<Void> updateMemberRole(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                               @RequestBody AuthDtos.RoleUpdateRequest request) {
        organizationService.updateFamMemberRole(authUser, id, request);
        return BaseResponse.success();
    }

    @DeleteMapping("/fam-members/{id}")
    public BaseResponse<Void> deleteMember(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        organizationService.deleteFamMember(authUser, id);
        return BaseResponse.success();
    }
}
