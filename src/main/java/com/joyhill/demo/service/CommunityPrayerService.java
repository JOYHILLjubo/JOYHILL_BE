package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.CommunityPrayer;
import com.joyhill.demo.domain.CommunityPrayerParticipant;
import com.joyhill.demo.domain.Role;
import com.joyhill.demo.repository.CommunityPrayerParticipantRepository;
import com.joyhill.demo.repository.CommunityPrayerRepository;
import com.joyhill.demo.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommunityPrayerService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int PAGE_SIZE = 30;

    private final CommunityPrayerRepository communityPrayerRepository;
    private final CommunityPrayerParticipantRepository communityPrayerParticipantRepository;

    public CommunityPrayerService(CommunityPrayerRepository communityPrayerRepository,
                                   CommunityPrayerParticipantRepository communityPrayerParticipantRepository) {
        this.communityPrayerRepository = communityPrayerRepository;
        this.communityPrayerParticipantRepository = communityPrayerParticipantRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long currentUserId) {
        List<CommunityPrayer> prayers = communityPrayerRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, PAGE_SIZE));

        List<Long> prayerIds = prayers.stream().map(CommunityPrayer::getId).toList();
        Set<Long> joinedIds = prayerIds.isEmpty()
                ? Set.of()
                : communityPrayerParticipantRepository.findByPrayerIdInAndUserId(prayerIds, currentUserId)
                        .stream().map(CommunityPrayerParticipant::getPrayerId).collect(Collectors.toSet());

        return prayers.stream().map(p -> toMap(p, joinedIds)).toList();
    }

    // (prayer_id, user_id) 조합으로 참여를 토글한다 - 이미 참여했으면 취소, 아니면 참여
    public Map<String, Object> togglePray(AuthUser authUser, Long prayerId) {
        if (!communityPrayerRepository.existsById(prayerId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "기도제목을 찾을 수 없습니다.");
        }

        boolean alreadyJoined = communityPrayerParticipantRepository
                .existsByPrayerIdAndUserId(prayerId, authUser.userId());

        if (alreadyJoined) {
            communityPrayerParticipantRepository.deleteByPrayerIdAndUserId(prayerId, authUser.userId());
        } else {
            communityPrayerParticipantRepository.save(new CommunityPrayerParticipant(prayerId, authUser.userId()));
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", prayerId);
        map.put("participantCount", communityPrayerParticipantRepository.countByPrayerId(prayerId));
        map.put("joinedByMe", !alreadyJoined);
        return map;
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
        return toMap(prayer, Set.of());
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

    private Map<String, Object> toMap(CommunityPrayer prayer, Set<Long> joinedIds) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", prayer.getId());
        map.put("userId", prayer.getUserId());
        map.put("content", prayer.getContent());
        map.put("createdAt", prayer.getCreatedAt() != null
                ? prayer.getCreatedAt().format(FORMATTER)
                : "");
        map.put("participantCount", communityPrayerParticipantRepository.countByPrayerId(prayer.getId()));
        map.put("joinedByMe", joinedIds.contains(prayer.getId()));
        return map;
    }
}
