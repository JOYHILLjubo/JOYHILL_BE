package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.CommunityPrayer;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.repository.CommunityPrayerRepository;
import com.joyhill.demo.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class CommunityPrayerService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int PAGE_SIZE = 30;

    private final CommunityPrayerRepository communityPrayerRepository;

    public CommunityPrayerService(CommunityPrayerRepository communityPrayerRepository) {
        this.communityPrayerRepository = communityPrayerRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return communityPrayerRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, PAGE_SIZE))
                .stream()
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> create(AuthUser authUser, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("기도제목을 입력해주세요.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("기도제목은 " + MAX_CONTENT_LENGTH + "자 이내로 입력해주세요.");
        }
        CommunityPrayer prayer = new CommunityPrayer();
        prayer.setUserId(authUser.userId());
        prayer.setContent(trimmed);
        communityPrayerRepository.save(prayer);
        return toMap(prayer);
    }

    public void delete(AuthUser authUser, Long id) {
        CommunityPrayer prayer = communityPrayerRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "기도제목을 찾을 수 없습니다."));

        boolean isMine = authUser.userId().equals(prayer.getUserId());
        boolean isPrivileged = authUser.role() == Role.village_leader
                || authUser.role() == Role.pastor
                || authUser.role() == Role.admin;

        if (!isMine && !isPrivileged) {
            throw new ApiException(ErrorCode.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        communityPrayerRepository.delete(prayer);
    }

    private Map<String, Object> toMap(CommunityPrayer prayer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", prayer.getId());
        map.put("userId", prayer.getUserId());
        map.put("content", prayer.getContent());
        map.put("createdAt", prayer.getCreatedAt() != null
                ? prayer.getCreatedAt().format(FORMATTER)
                : "");
        return map;
    }
}
