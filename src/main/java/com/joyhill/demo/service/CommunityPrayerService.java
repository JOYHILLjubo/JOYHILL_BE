package com.joyhill.demo.service;

import com.joyhill.demo.domain.CommunityPrayer;
import com.joyhill.demo.repository.CommunityPrayerRepository;
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

    public Map<String, Object> create(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("기도제목을 입력해주세요.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("기도제목은 " + MAX_CONTENT_LENGTH + "자 이내로 입력해주세요.");
        }
        CommunityPrayer prayer = new CommunityPrayer();
        prayer.setContent(trimmed);
        communityPrayerRepository.save(prayer);
        return toMap(prayer);
    }

    private Map<String, Object> toMap(CommunityPrayer prayer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", prayer.getId());
        map.put("content", prayer.getContent());
        map.put("createdAt", prayer.getCreatedAt() != null
                ? prayer.getCreatedAt().format(FORMATTER)
                : "");
        return map;
    }
}
