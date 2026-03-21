package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Newcomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewcomerRepository extends JpaRepository<Newcomer, Long> {
}
