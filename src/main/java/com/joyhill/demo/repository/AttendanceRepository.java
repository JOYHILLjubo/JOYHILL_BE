package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByFamNameAndDate(String famName, LocalDate date);
    Optional<Attendance> findByFamMemberIdAndDate(Long famMemberId, LocalDate date);
    List<Attendance> findByFamNameAndDateBetween(String famName, LocalDate from, LocalDate to);
    List<Attendance> findByFamNameInAndDateBetween(List<String> famNames, LocalDate from, LocalDate to);
    List<Attendance> findByDateBetween(LocalDate from, LocalDate to);
}
