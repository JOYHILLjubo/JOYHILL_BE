package com.joyhill.demo.service;

import com.joyhill.demo.common.api.ErrorCode;
import com.joyhill.demo.common.exception.ApiException;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.Newcomer;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.NewcomerRepository;
import com.joyhill.demo.security.AuthUser;
import com.joyhill.demo.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NewcomerService {

    private final NewcomerRepository newcomerRepository;
    private final FamRepository famRepository;
    private final AccessGuard accessGuard;

    public NewcomerService(NewcomerRepository newcomerRepository, FamRepository famRepository, AccessGuard accessGuard) {
        this.newcomerRepository = newcomerRepository;
        this.famRepository = famRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(AuthUser authUser) {
        accessGuard.requireNewcomerManager(authUser);
        return newcomerRepository.findAll().stream().map(this::toMap).toList();
    }

    public Map<String, Object> create(AuthUser authUser, AuthDtos.NewcomerRequest request) {
        accessGuard.requireNewcomerManager(authUser);
        Newcomer newcomer = new Newcomer();
        newcomer.setName(request.name());
        newcomer.setPhone(PhoneUtils.normalize(request.phone()));
        newcomer.setBirth(request.birth());
        newcomer.setRegisteredAt(request.registeredAt());
        newcomer.setNote(request.note());
        newcomerRepository.save(newcomer);
        return toMap(newcomer);
    }

    public Map<String, Object> assignFam(AuthUser authUser, Long id, AuthDtos.NewcomerFamAssignRequest request) {
        accessGuard.requireRoleAtLeast(authUser, com.joyhill.demo.domain.Role.leader);
        accessGuard.requireFamScope(authUser, request.famName());
        famRepository.findByName(request.famName()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "팸을 찾을 수 없습니다."));
        Newcomer newcomer = getNewcomer(id);
        newcomer.setFamName(request.famName());
        return toMap(newcomer);
    }

    public void delete(AuthUser authUser, Long id) {
        accessGuard.requireRoleAtLeast(authUser, com.joyhill.demo.domain.Role.leader);
        newcomerRepository.delete(getNewcomer(id));
    }

    private Newcomer getNewcomer(Long id) {
        return newcomerRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "새가족을 찾을 수 없습니다."));
    }

    private Map<String, Object> toMap(Newcomer newcomer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", newcomer.getId());
        map.put("name", newcomer.getName());
        map.put("phone", newcomer.getPhone());
        map.put("birth", newcomer.getBirth());
        map.put("registeredAt", newcomer.getRegisteredAt());
        map.put("note", newcomer.getNote());
        map.put("famName", newcomer.getFamName());
        map.put("createdAt", newcomer.getCreatedAt());
        return map;
    }
}
