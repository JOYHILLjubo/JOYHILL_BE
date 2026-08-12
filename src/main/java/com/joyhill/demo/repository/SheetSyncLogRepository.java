package com.joyhill.demo.repository;

import com.joyhill.demo.domain.SheetSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SheetSyncLogRepository extends JpaRepository<SheetSyncLog, Long> {
    Optional<SheetSyncLog> findBySyncType(String syncType);
}
