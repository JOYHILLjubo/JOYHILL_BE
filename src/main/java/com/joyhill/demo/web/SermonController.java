package com.joyhill.demo.web;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.service.SermonService;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sermon")
public class SermonController {

    private final SermonService sermonService;

    public SermonController(SermonService sermonService) {
        this.sermonService = sermonService;
    }

    @GetMapping("/latest")
    public BaseResponse<Map<String, Object>> latest() {
        return BaseResponse.success(sermonService.latest());
    }

    @PostMapping
    public BaseResponse<Map<String, Object>> save(@AuthenticationPrincipal AuthUser authUser,
                                                  @RequestBody AuthDtos.SermonRequest request) {
        return BaseResponse.success(sermonService.save(authUser, request));
    }
}
