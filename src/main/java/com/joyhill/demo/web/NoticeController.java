package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.NoticeService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public BaseResponse<Map<String, Object>> list(@RequestParam(required = false) String tag,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return BaseResponse.success(noticeService.list(tag, search, page, size));
    }

    @GetMapping("/{id}")
    public BaseResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return BaseResponse.success(noticeService.detail(id));
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @RequestBody AuthDtos.NoticeRequest request) {
        return BaseResponse.success(noticeService.create(authUser, request));
    }

    @PutMapping("/{id}")
    public BaseResponse<Map<String, Object>> update(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id,
                                                    @RequestBody AuthDtos.NoticeRequest request) {
        return BaseResponse.success(noticeService.update(authUser, id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        noticeService.delete(authUser, id);
        return BaseResponse.success();
    }
}
