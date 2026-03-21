package com.joyhill.demo.security;

import com.joyhill.demo.domain.Role;

public record AuthUser(Long userId, String name, Role role, String famName, String villageName) {
}
