package com.joyhill.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fams")
public class Fam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "village_name", nullable = false, length = 50)
    private String villageName;

    @Column(name = "leader_name", length = 50)
    private String leaderName;

    public Fam() {
    }

    public Fam(String name, String villageName, String leaderName) {
        this.name = name;
        this.villageName = villageName;
        this.leaderName = leaderName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }
}
