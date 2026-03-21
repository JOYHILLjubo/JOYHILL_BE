package com.joyhill.demo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "team_roles")
public class TeamRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;

    @Column(name = "is_leader", nullable = false)
    private boolean leader;

    public TeamRole() {
    }

    public TeamRole(Long userId, String teamName, boolean leader) {
        this.userId = userId;
        this.teamName = teamName;
        this.leader = leader;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTeamName() {
        return teamName;
    }

    public boolean isLeader() {
        return leader;
    }
}
