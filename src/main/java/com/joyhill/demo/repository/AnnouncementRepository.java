package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    // 현재 활성 광고: startDate가 null이거나 오늘 이전, endDate가 null이거나 오늘 이후
    List<Announcement> findByStartDateIsNullOrStartDateLessThanEqual(LocalDate today);
}
