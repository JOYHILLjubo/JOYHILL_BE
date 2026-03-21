package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Village;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VillageRepository extends JpaRepository<Village, Long> {
    Optional<Village> findByName(String name);
    void deleteByName(String name);
}
