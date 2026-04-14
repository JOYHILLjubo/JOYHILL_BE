package com.joyhill.demo.repository;

import com.joyhill.demo.domain.CommunityPrayer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPrayerRepository extends JpaRepository<CommunityPrayer, Long> {
    List<CommunityPrayer> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
