package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.SermonNoteService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sermon-notes")
public class SermonNoteController {

    private final SermonNoteService sermonNoteService;

    public SermonNoteController(SermonNoteService sermonNoteService) {
        this.sermonNoteService = sermonNoteService;
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal AuthUser authUser) {
        return BaseResponse.success(sermonNoteService.list(authUser));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                     @RequestBody AuthDtos.SermonNoteRequest request) {
        return BaseResponse.success(sermonNoteService.create(authUser, request));
    }

    @PutMapping("/{id}")
    public BaseResponse<Map<String, Object>> update(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                     @RequestBody AuthDtos.SermonNoteRequest request) {
        return BaseResponse.success(sermonNoteService.update(authUser, id, request));
    }

    @PatchMapping("/{id}/favorite")
    public BaseResponse<Map<String, Object>> toggleFavorite(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        return BaseResponse.success(sermonNoteService.toggleFavorite(authUser, id));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        sermonNoteService.delete(authUser, id);
        return BaseResponse.success();
    }
}
