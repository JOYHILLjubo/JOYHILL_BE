package com.joyhill.demo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "community_prayers")
public class CommunityPrayer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public Long getId() { return id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
