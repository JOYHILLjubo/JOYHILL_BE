package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fam_member_id → user_id 로 변경 (users 테이블 참조)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fam_name", nullable = false, length = 50)
    private String famName;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "worship_present", nullable = false)
    private boolean worshipPresent;

    @Column(name = "online_present", nullable = false)
    private boolean onlinePresent;

    @Column(name = "fam_present", nullable = false)
    private boolean famPresent;

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFamName() { return famName; }
    public void setFamName(String famName) { this.famName = famName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isWorshipPresent() { return worshipPresent; }
    public void setWorshipPresent(boolean worshipPresent) { this.worshipPresent = worshipPresent; }

    public boolean isOnlinePresent() { return onlinePresent; }
    public void setOnlinePresent(boolean onlinePresent) { this.onlinePresent = onlinePresent; }

    public boolean isFamPresent() { return famPresent; }
    public void setFamPresent(boolean famPresent) { this.famPresent = famPresent; }
}
