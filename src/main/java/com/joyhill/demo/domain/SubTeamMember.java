package com.joyhill.demo.domain;
import jakarta.persistence.*;
@Entity
@Table(name = "sub_team_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sub_team_id", "user_id"}))
public class SubTeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sub_team_id", nullable = false)
    private Long subTeamId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    public SubTeamMember() {}
    public SubTeamMember(Long subTeamId, Long userId) {
        this.subTeamId = subTeamId;
        this.userId = userId;
    }
    public Long getId() { return id; }
    public Long getSubTeamId() { return subTeamId; }
    public Long getUserId() { return userId; }
}
