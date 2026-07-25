package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.domain.SermonNoteFolder;
import com.joyhill.demo.repository.SermonNoteFolderRepository;
import com.joyhill.demo.repository.SermonNoteRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SermonNoteFolderService {

    private final SermonNoteFolderRepository sermonNoteFolderRepository;
    private final SermonNoteRepository sermonNoteRepository;

    public SermonNoteFolderService(SermonNoteFolderRepository sermonNoteFolderRepository,
                                   SermonNoteRepository sermonNoteRepository) {
        this.sermonNoteFolderRepository = sermonNoteFolderRepository;
        this.sermonNoteRepository = sermonNoteRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser) {
        return sermonNoteFolderRepository.findByUserIdOrderByIdAsc(authUser.userId())
                .stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public long unclassifiedCount(AuthUser authUser) {
        return sermonNoteRepository.countByUserIdAndFolderIdIsNull(authUser.userId());
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.SermonNoteFolderRequest request) {
        String name = validateName(request.name());
        SermonNoteFolder folder = new SermonNoteFolder();
        folder.setUserId(authUser.userId());
        folder.setName(name);
        sermonNoteFolderRepository.save(folder);
        return toMap(folder);
    }

    public Map<String, Object> rename(AuthUser authUser, Long id, AuthDtos.SermonNoteFolderRequest request) {
        SermonNoteFolder folder = getOwnedFolder(authUser, id);
        folder.setName(validateName(request.name()));
        return toMap(folder);
    }

    // 폴더를 지우면 그 안의 노트는 삭제되지 않고 미분류로 이동한다.
    public void delete(AuthUser authUser, Long id) {
        SermonNoteFolder folder = getOwnedFolder(authUser, id);
        sermonNoteRepository.unassignFolder(folder.getId());
        sermonNoteFolderRepository.delete(folder);
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "폴더 이름을 입력해주세요.");
        }
        return name.trim();
    }

    private SermonNoteFolder getOwnedFolder(AuthUser authUser, Long id) {
        return sermonNoteFolderRepository.findById(id)
                .filter(f -> f.getUserId().equals(authUser.userId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "폴더를 찾을 수 없습니다."));
    }

    private Map<String, Object> toMap(SermonNoteFolder folder) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", folder.getId());
        map.put("name", folder.getName());
        map.put("noteCount", sermonNoteRepository.countByUserIdAndFolderId(folder.getUserId(), folder.getId()));
        return map;
    }
}
