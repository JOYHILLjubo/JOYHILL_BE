package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "sermons")
public class Sermon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String verse;

    @Column(nullable = false, length = 50)
    private String preacher;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "sermon_date", nullable = false)
    private LocalDate sermonDate;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVerse() {
        return verse;
    }

    public void setVerse(String verse) {
        this.verse = verse;
    }

    public String getPreacher() {
        return preacher;
    }

    public void setPreacher(String preacher) {
        this.preacher = preacher;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDate getSermonDate() {
        return sermonDate;
    }

    public void setSermonDate(LocalDate sermonDate) {
        this.sermonDate = sermonDate;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
