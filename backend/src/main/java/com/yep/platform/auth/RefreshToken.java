package com.yep.platform.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "token_hash", nullable = false) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected RefreshToken() {}
    RefreshToken(Long userId, String tokenHash, Instant expiresAt) { this.userId = userId; this.tokenHash = tokenHash; this.expiresAt = expiresAt; }
    Long getUserId() { return userId; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getRevokedAt() { return revokedAt; }
    void revoke() { this.revokedAt = Instant.now(); }
}
