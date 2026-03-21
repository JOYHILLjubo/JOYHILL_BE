package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Sermon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SermonRepository extends JpaRepository<Sermon, Long> {
    Optional<Sermon> findTopByOrderBySermonDateDesc();
}
