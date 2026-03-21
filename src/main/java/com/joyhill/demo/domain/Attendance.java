package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"fam_member_id", "date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fam_member_id", nullable = false)
    private Long famMemberId;

    @Column(name = "fam_name", nullable = false, length = 50)
    private String famName;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "worship_present", nullable = false)
    private boolean worshipPresent;

    @Column(name = "fam_present", nullable = false)
    private boolean famPresent;

    public Long getId() {
        return id;
    }

    public Long getFamMemberId() {
        return famMemberId;
    }

    public void setFamMemberId(Long famMemberId) {
        this.famMemberId = famMemberId;
    }

    public String getFamName() {
        return famName;
    }

    public void setFamName(String famName) {
        this.famName = famName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isWorshipPresent() {
        return worshipPresent;
    }

    public void setWorshipPresent(boolean worshipPresent) {
        this.worshipPresent = worshipPresent;
    }

    public boolean isFamPresent() {
        return famPresent;
    }

    public void setFamPresent(boolean famPresent) {
        this.famPresent = famPresent;
    }
}
