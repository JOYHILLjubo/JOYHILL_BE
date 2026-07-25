package com.joyhill.demo.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "sermon_notes")
public class SermonNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 미분류(폴더 없음)일 경우 null
    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(length = 200)
    private String title;

    // 서식(굵게/하이라이트/글자색)이 적용된 리치 텍스트 에디터의 HTML
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 콤마로 구분된 말씀구절 태그 (예: "예레미야 29:11,야고보서 1:4")
    @Column(name = "verse_tags", length = 300)
    private String verseTags;

    // 적용할 점 체크리스트, JSON 배열 문자열 (예: [{"text":"...","done":true}])
    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(nullable = false)
    private boolean favorite;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public LocalDate getNoteDate() {
        return noteDate;
    }

    public void setNoteDate(LocalDate noteDate) {
        this.noteDate = noteDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVerseTags() {
        return verseTags;
    }

    public void setVerseTags(String verseTags) {
        this.verseTags = verseTags;
    }

    public String getChecklistJson() {
        return checklistJson;
    }

    public void setChecklistJson(String checklistJson) {
        this.checklistJson = checklistJson;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
