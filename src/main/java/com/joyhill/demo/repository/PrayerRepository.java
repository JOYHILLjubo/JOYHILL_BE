package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Prayer;
import com.joyhill.demo.domain.PrayerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrayerRepository extends JpaRepository<Prayer, Long> {
    List<Prayer> findByFamNameAndYearAndMonthAndWeek(String famName, int year, int month, Integer week);
    List<Prayer> findByFamNameAndYearAndMonth(String famName, int year, int month);
    Optional<Prayer> findByFamNameAndYearAndMonthAndType(String famName, int year, int month, PrayerType type);
}
