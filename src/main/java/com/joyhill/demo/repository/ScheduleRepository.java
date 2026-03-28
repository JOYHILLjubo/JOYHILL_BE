package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDateGreaterThanEqualOrderByDateAsc(LocalDate from);
}
