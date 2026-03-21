package com.joyhill.demo.service;

import com.joyhill.demo.domain.Sermon;
import com.joyhill.demo.repository.SermonRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class SermonService {

    private final SermonRepository sermonRepository;
    private final AccessGuard accessGuard;

    public SermonService(SermonRepository sermonRepository, AccessGuard accessGuard) {
        this.sermonRepository = sermonRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> latest() {
        return sermonRepository.findTopByOrderBySermonDateDesc().map(this::toMap).orElse(Map.of());
    }

    public Map<String, Object> save(AuthUser authUser, AuthDtos.SermonRequest request) {
        accessGuard.requirePastorOrAdmin(authUser);
        Sermon sermon = sermonRepository.findTopByOrderBySermonDateDesc()
                .filter(existing -> existing.getSermonDate().equals(request.sermonDate()))
                .orElseGet(Sermon::new);
        sermon.setTitle(request.title());
        sermon.setVerse(request.verse());
        sermon.setPreacher(request.preacher());
        sermon.setYoutubeUrl(request.youtubeUrl());
        sermon.setSummary(request.summary());
        sermon.setSermonDate(request.sermonDate());
        sermon.setUploadedBy(authUser.userId());
        sermonRepository.save(sermon);
        return toMap(sermon);
    }

    private Map<String, Object> toMap(Sermon sermon) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sermon.getId());
        map.put("title", sermon.getTitle());
        map.put("verse", sermon.getVerse());
        map.put("preacher", sermon.getPreacher());
        map.put("youtubeUrl", sermon.getYoutubeUrl());
        map.put("summary", sermon.getSummary());
        map.put("sermonDate", sermon.getSermonDate());
        map.put("uploadedBy", sermon.getUploadedBy());
        return map;
    }
}
