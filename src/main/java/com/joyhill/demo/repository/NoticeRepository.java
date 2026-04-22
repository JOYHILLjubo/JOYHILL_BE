package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 고정 공지 먼저, 그다음 최신 순 정렬 포함 (공백 무시 검색)
    @Query(value = "SELECT * FROM notice WHERE REPLACE(title, ' ', '') ILIKE '%' || REPLACE(:search, ' ', '') || '%' ORDER BY pinned DESC, created_at DESC",
           countQuery = "SELECT COUNT(*) FROM notice WHERE REPLACE(title, ' ', '') ILIKE '%' || REPLACE(:search, ' ', '') || '%'",
           nativeQuery = true)
    Page<Notice> findByTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT * FROM notice WHERE tag LIKE %:tag% AND REPLACE(title, ' ', '') ILIKE '%' || REPLACE(:search, ' ', '') || '%' ORDER BY pinned DESC, created_at DESC",
           countQuery = "SELECT COUNT(*) FROM notice WHERE tag LIKE %:tag% AND REPLACE(title, ' ', '') ILIKE '%' || REPLACE(:search, ' ', '') || '%'",
           nativeQuery = true)
    Page<Notice> findByTagAndTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(@Param("tag") String tag, @Param("search") String search, Pageable pageable);
}
