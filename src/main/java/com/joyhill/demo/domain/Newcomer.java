package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "newcomers")
public class Newcomer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    private LocalDate birth;

    @Column(name = "registered_at", nullable = false)
    private LocalDate registeredAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "fam_name", length = 50)
    private String famName;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirth() {
        return birth;
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getFamName() {
        return famName;
    }

    public void setFamName(String famName) {
        this.famName = famName;
    }
}
