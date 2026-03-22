package com.joyhill.demo.repository;

import com.joyhill.demo.domain.FamMember;
import com.joyhill.demo.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamMemberRepository extends JpaRepository<FamMember, Long> {
    List<FamMember> findByFamName(String famName);
    long countByFamName(String famName);
    long countByFamNameAndIdNot(String famName, Long id);
    // 강등 체크용: 해당 팸에서 특정 이름을 제외한 팸원 수 (리더 본인 제외)
    long countByFamNameAndNameNot(String famName, String name);
    List<FamMember> findByFamNameIn(List<String> famNames);
    List<FamMember> findByRole(Role role);
}
