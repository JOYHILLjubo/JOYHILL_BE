package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByTagContainingIgnoreCaseAndTitleContainingIgnoreCase(String tag, String search, Pageable pageable);
}
