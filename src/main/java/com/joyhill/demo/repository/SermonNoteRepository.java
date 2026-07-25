package com.joyhill.demo.repository;

import com.joyhill.demo.domain.SermonNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SermonNoteRepository extends JpaRepository<SermonNote, Long> {
    List<SermonNote> findByUserIdOrderByNoteDateDescIdDesc(Long userId);

    List<SermonNote> findByUserIdAndFolderIdOrderByNoteDateDescIdDesc(Long userId, Long folderId);

    List<SermonNote> findByUserIdAndFolderIdIsNullOrderByNoteDateDescIdDesc(Long userId);

    long countByUserIdAndFolderId(Long userId, Long folderId);

    long countByUserIdAndFolderIdIsNull(Long userId);

    @Modifying
    @Query("UPDATE SermonNote n SET n.folderId = null WHERE n.folderId = :folderId")
    void unassignFolder(@Param("folderId") Long folderId);
}
