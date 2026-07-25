package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.SermonNoteFolderService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sermon-note-folders")
public class SermonNoteFolderController {

    private final SermonNoteFolderService sermonNoteFolderService;

    public SermonNoteFolderController(SermonNoteFolderService sermonNoteFolderService) {
        this.sermonNoteFolderService = sermonNoteFolderService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(sermonNoteFolderService.list(authUser));
    }

    @GetMapping("/unclassified-count")
    public BaseResponse<Long> unclassifiedCount(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(sermonNoteFolderService.unclassifiedCount(authUser));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                     @RequestBody AuthDtos.SermonNoteFolderRequest request) {
        return BaseResponse.success(sermonNoteFolderService.create(authUser, request));
    }

    @PutMapping("/{id}")
    public BaseResponse<Map<String, Object>> rename(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                     @RequestBody AuthDtos.SermonNoteFolderRequest request) {
        return BaseResponse.success(sermonNoteFolderService.rename(authUser, id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        sermonNoteFolderService.delete(authUser, id);
        return BaseResponse.success();
    }
}
