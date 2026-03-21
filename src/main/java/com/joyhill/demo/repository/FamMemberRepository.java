package com.joyhill.demo.repository;

import com.joyhill.demo.domain.FamMember;
import com.joyhill.demo.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamMemberRepository extends JpaRepository<FamMember, Long> {
    List<FamMember> findByFamName(String famName);
    long countByFamName(String famName);
    long countByFamNameAndIdNot(String famName, Long id);
    List<FamMember> findByFamNameIn(List<String> famNames);
    List<FamMember> findByRole(Role role);
}
