package com.joyhill.demo.domain;
import jakarta.persistence.*;
@Entity
@Table(name = "sub_teams",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_name", "sub_team_name"}))
public class SubTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;
    @Column(name = "sub_team_name", nullable = false, length = 50)
    private String subTeamName;
    @Column(name = "leader_user_id")
    private Long leaderUserId;
    public SubTeam() {}
    public SubTeam(String teamName, String subTeamName) {
        this.teamName = teamName;
        this.subTeamName = subTeamName;
    }
    public Long getId() { return id; }
    public String getTeamName() { return teamName; }
    public String getSubTeamName() { return subTeamName; }
    public Long getLeaderUserId() { return leaderUserId; }
    public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }
}
