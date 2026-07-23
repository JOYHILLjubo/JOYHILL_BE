package com.joyhill.demo.domain;
import jakarta.persistence.*;
@Entity
@Table(name = "community_prayer_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"prayer_id", "user_id"}))
public class CommunityPrayerParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "prayer_id", nullable = false)
    private Long prayerId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    public CommunityPrayerParticipant() {}
    public CommunityPrayerParticipant(Long prayerId, Long userId) {
        this.prayerId = prayerId;
        this.userId = userId;
    }
    public Long getId() { return id; }
    public Long getPrayerId() { return prayerId; }
    public Long getUserId() { return userId; }
}
