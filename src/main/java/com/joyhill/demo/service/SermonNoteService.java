package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.SermonNote;
import com.joyhill.demo.repository.SermonNoteRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SermonNoteService {

    private final SermonNoteRepository sermonNoteRepository;

    public SermonNoteService(SermonNoteRepository sermonNoteRepository) {
        this.sermonNoteRepository = sermonNoteRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser, Long folderId, boolean unclassified) {
        List<SermonNote> notes;
        if (unclassified) {
            notes = sermonNoteRepository.findByUserIdAndFolderIdIsNullOrderByNoteDateDescIdDesc(authUser.userId());
        } else if (folderId != null) {
            notes = sermonNoteRepository.findByUserIdAndFolderIdOrderByNoteDateDescIdDesc(authUser.userId(), folderId);
        } else {
            notes = sermonNoteRepository.findByUserIdOrderByNoteDateDescIdDesc(authUser.userId());
        }
        return notes.stream().map(this::toMap).toList();
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.SermonNoteRequest request) {
        SermonNote note = new SermonNote();
        note.setUserId(authUser.userId());
        apply(note, request);
        sermonNoteRepository.save(note);
        return toMap(note);
    }

    public Map<String, Object> update(AuthUser authUser, Long id, AuthDtos.SermonNoteRequest request) {
        SermonNote note = getOwnedNote(authUser, id);
        apply(note, request);
        return toMap(note);
    }

    public Map<String, Object> toggleFavorite(AuthUser authUser, Long id) {
        SermonNote note = getOwnedNote(authUser, id);
        note.setFavorite(!note.isFavorite());
        return toMap(note);
    }

    public void delete(AuthUser authUser, Long id) {
        sermonNoteRepository.delete(getOwnedNote(authUser, id));
    }

    // 완전 비공개 노트이므로 소유자가 아니면 존재 자체를 알리지 않고 NOT_FOUND로 응답한다.
    private SermonNote getOwnedNote(AuthUser authUser, Long id) {
        SermonNote note = sermonNoteRepository.findById(id)
                .filter(n -> n.getUserId().equals(authUser.userId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "노트를 찾을 수 없습니다."));
        return note;
    }

    private void apply(SermonNote note, AuthDtos.SermonNoteRequest request) {
        note.setNoteDate(request.noteDate() != null ? request.noteDate() : LocalDate.now());
        note.setTitle(request.title());
        note.setContent(request.content() != null ? request.content() : "");
        note.setVerseTags(request.verseTags());
        note.setChecklistJson(request.checklistJson());
        note.setFolderId(request.folderId());
    }

    private Map<String, Object> toMap(SermonNote note) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", note.getId());
        map.put("folderId", note.getFolderId());
        map.put("noteDate", note.getNoteDate());
        map.put("title", note.getTitle());
        map.put("content", note.getContent());
        map.put("verseTags", note.getVerseTags());
        map.put("checklistJson", note.getChecklistJson());
        map.put("favorite", note.isFavorite());
        map.put("createdAt", note.getCreatedAt());
        return map;
    }
}
