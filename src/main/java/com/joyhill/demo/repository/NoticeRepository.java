package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 고정 공지 먼저, 그다음 최신 순 정렬 포함
    Page<Notice> findByTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(String search, Pageable pageable);
    Page<Notice> findByTagAndTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(String tag, String search, Pageable pageable);
}
