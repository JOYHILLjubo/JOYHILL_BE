package com.joyhill.demo.repository;

import com.joyhill.demo.domain.SermonNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SermonNoteRepository extends JpaRepository<SermonNote, Long> {
    List<SermonNote> findByUserIdOrderByNoteDateDescIdDesc(Long userId);
}
