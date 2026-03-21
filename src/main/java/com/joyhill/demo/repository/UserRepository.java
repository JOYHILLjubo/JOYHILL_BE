package com.joyhill.demo.repository;

import com.joyhill.demo.domain.Role;
import com.joyhill.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    List<User> findByRole(Role role);
    List<User> findByNameContainingIgnoreCase(String search);
    List<User> findByRoleAndNameContainingIgnoreCase(Role role, String search);
    boolean existsByPhone(String phone);
}
