package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 구글시트 백업이 마지막으로 성공한 시각. 백업 종류(회원 정보/출석 통계)당 한 행만 두고 갱신한다.
 */
@Entity
@Table(name = "sheet_sync_logs")
public class SheetSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "members" 또는 "attendance" */
    @Column(name = "sync_type", nullable = false, unique = true, length = 30)
    private String syncType;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    /** 그때 시트에 쓴 인원 수 (헤더 제외) */
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    public SheetSyncLog() {
    }

    public SheetSyncLog(String syncType, LocalDateTime syncedAt, int rowCount) {
        this.syncType = syncType;
        this.syncedAt = syncedAt;
        this.rowCount = rowCount;
    }

    public Long getId() {
        return id;
    }

    public String getSyncType() {
        return syncType;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }
}
