package com.joyhill.demo.repository;

import com.joyhill.demo.domain.CommunityPrayerParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPrayerParticipantRepository extends JpaRepository<CommunityPrayerParticipant, Long> {
    boolean existsByPrayerIdAndUserId(Long prayerId, Long userId);
    long countByPrayerId(Long prayerId);
    void deleteByPrayerIdAndUserId(Long prayerId, Long userId);
    List<CommunityPrayerParticipant> findByPrayerIdInAndUserId(List<Long> prayerIds, Long userId);
}
