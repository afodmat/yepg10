package com.yep.platform.security;

import com.yep.platform.user.Role;

/** Authenticated principal derived from the verified JWT (never from client-supplied fields). */
public record AuthUser(Long id, String email, Role role) {}
