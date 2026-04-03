package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.Notice;
import com.joyhill.demo.repository.NoticeRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class NoticeService {

    private static final Set<String> ALLOWED_TAGS = Set.of("행사", "안내", "소식", "요청");

    private final NoticeRepository noticeRepository;
    private final AccessGuard accessGuard;

    public NoticeService(NoticeRepository noticeRepository, AccessGuard accessGuard) {
        this.noticeRepository = noticeRepository;
        this.accessGuard = accessGuard;
    }

    // 공지 목록 — pinned 먼저, 최신 순 정렬
    @Transactional(readOnly = true)
    public Map<String, Object> list(String tag, String search, int page, int size) {
        String keyword = search == null ? "" : search;
        Page<Notice> result;
        if (tag != null && !tag.isBlank()) {
            result = noticeRepository.findByTagAndTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(
                    tag, keyword, PageRequest.of(page, size));
        } else {
            result = noticeRepository.findByTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(
                    keyword, PageRequest.of(page, size));
        }
        return Map.of(
                "content", result.getContent().stream().map(this::toMap).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        return toMap(getNotice(id));
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.NoticeRequest request) {
        accessGuard.requireNoticeWriter(authUser);
        validateTag(request.tag());
        Notice notice = new Notice();
        apply(notice, authUser, request);
        noticeRepository.save(notice);
        return toMap(notice);
    }

    public Map<String, Object> update(AuthUser authUser, Long id, AuthDtos.NoticeRequest request) {
        Notice notice = getNotice(id);
        if (!notice.getUserId().equals(authUser.userId()) && authUser.role() != com.joyhill.demo.domain.Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "작성자 또는 관리자만 수정할 수 있습니다.");
        }
        validateTag(request.tag());
        apply(notice, authUser, request);
        return toMap(notice);
    }

    public void delete(AuthUser authUser, Long id) {
        Notice notice = getNotice(id);
        if (!notice.getUserId().equals(authUser.userId()) && authUser.role() != com.joyhill.demo.domain.Role.admin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "작성자 또는 관리자만 삭제할 수 있습니다.");
        }
        noticeRepository.delete(notice);
    }

    private void validateTag(String tag) {
        if (tag == null || !ALLOWED_TAGS.contains(tag)) {
            throw new ApiException(ErrorCode.INVALID_TAG, "태그는 행사, 안내, 소식, 요청 중 하나여야 합니다.");
        }
    }

    private Notice getNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }

    private void apply(Notice notice, AuthUser authUser, AuthDtos.NoticeRequest request) {
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setTag(request.tag());
        notice.setTeamTag(request.teamTag());
        notice.setPinned(request.pinned());
        notice.setDeadline(request.deadline());
        notice.setFileUrl(request.fileUrl());
        notice.setAuthor(authUser.name());
        notice.setUserId(authUser.userId());
    }

    private Map<String, Object> toMap(Notice notice) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notice.getId());
        map.put("title", notice.getTitle());
        map.put("content", notice.getContent());
        map.put("author", notice.getAuthor());
        map.put("userId", notice.getUserId());
        map.put("tag", notice.getTag());
        map.put("teamTag", notice.getTeamTag());
        map.put("pinned", notice.isPinned());
        map.put("deadline", notice.getDeadline());
        map.put("fileUrl", notice.getFileUrl());
        map.put("createdAt", notice.getCreatedAt());
        return map;
    }
}
