package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.NewcomerService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newcomers")
public class NewcomerController {

    private final NewcomerService newcomerService;

    public NewcomerController(NewcomerService newcomerService) {
        this.newcomerService = newcomerService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(newcomerService.list(authUser));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody AuthDtos.NewcomerRequest request) {
        return BaseResponse.success(newcomerService.create(authUser, request));
    }

    @PatchMapping("/{id}/fam")
    public BaseResponse<Map<String, Object>> assignFam(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                       @RequestBody AuthDtos.NewcomerFamAssignRequest request) {
        return BaseResponse.success(newcomerService.assignFam(authUser, id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        newcomerService.delete(authUser, id);
        return BaseResponse.success();
    }
}
