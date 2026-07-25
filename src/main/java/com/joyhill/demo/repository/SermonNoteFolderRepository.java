package com.joyhill.demo.repository;

import com.joyhill.demo.domain.SermonNoteFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SermonNoteFolderRepository extends JpaRepository<SermonNoteFolder, Long> {
    List<SermonNoteFolder> findByUserIdOrderByIdAsc(Long userId);
}
