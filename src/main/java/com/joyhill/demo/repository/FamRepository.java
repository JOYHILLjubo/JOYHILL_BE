package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Fam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamRepository extends JpaRepository<Fam, Long> {
    Optional<Fam> findByName(String name);
    List<Fam> findByVillageName(String villageName);
    long countByVillageName(String villageName);
    void deleteByName(String name);
}
