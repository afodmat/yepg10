package com.yep.platform.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Public seam so other modules can sign a user out everywhere without seeing token internals. */
@Component
public class SessionRevoker {
    private final RefreshTokenRepository repo;
    public SessionRevoker(RefreshTokenRepository repo) { this.repo = repo; }
    @Transactional public void revokeAll(Long userId) { repo.revokeAllForUser(userId); }
}
