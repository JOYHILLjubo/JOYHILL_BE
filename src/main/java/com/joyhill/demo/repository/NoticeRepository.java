package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 태그 없을 때: 제목 검색만
    Page<Notice> findByTitleContainingIgnoreCase(String search, Pageable pageable);
    // 태그 있을 때: 태그 exact match + 제목 검색
    Page<Notice> findByTagAndTitleContainingIgnoreCase(String tag, String search, Pageable pageable);
}
