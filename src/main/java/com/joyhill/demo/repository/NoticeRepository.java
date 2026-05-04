package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 고정 공지 먼저, 그다음 최신 순 정렬 포함
    Page<Notice> findByTitleContainingIgnoreCaseOrderByPinnedDescCreatedAtDesc(String search, Pageable pageable);

    // 콤마로 저장된 태그 중 하나라도 일치하면 조회 (예: "행사,안내" 중 "행사" 포함 여부)
    @Query("SELECT n FROM Notice n WHERE " +
           "(n.tag = :tag OR n.tag LIKE CONCAT(:tag, ',%') OR n.tag LIKE CONCAT('%,', :tag) OR n.tag LIKE CONCAT('%,', :tag, ',%')) " +
           "AND LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY n.pinned DESC, n.createdAt DESC")
    Page<Notice> findByTagContainingAndTitleContainingIgnoreCase(@Param("tag") String tag, @Param("search") String search, Pageable pageable);
}
