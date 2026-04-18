package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.NoticeService;
import com.joyhill.demo.service.S3Service;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final S3Service s3Service;

    public NoticeController(NoticeService noticeService, S3Service s3Service) {
        this.noticeService = noticeService;
        this.s3Service = s3Service;
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

    /**
     * 공지 이미지 업로드
     * POST /api/notices/image
     * multipart/form-data, field name: image
     * → { imageUrl: "https://..." }
     */
    @PostMapping("/image")
    public BaseResponse<java.util.Map<String, String>> uploadImage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam("image") MultipartFile image) {
        String url = s3Service.upload(image, "notices");
        return BaseResponse.success(java.util.Map.of("imageUrl", url));
    }
}
