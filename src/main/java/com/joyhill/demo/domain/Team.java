package com.joyhill.demo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, unique = true, length = 50)
    private String teamName;

    @Column(columnDefinition = "TEXT")
    private String intro;

    public Team() {}

    public Team(String teamName, String intro) {
        this.teamName = teamName;
        this.intro = intro;
    }

    public Long getId() { return id; }
    public String getTeamName() { return teamName; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
}
