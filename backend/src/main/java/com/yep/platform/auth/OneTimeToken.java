package com.yep.platform.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "one_time_tokens")
public class OneTimeToken {
    public enum Type { ACTIVATION, PASSWORD_RESET }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    @Column(name = "token_hash", nullable = false) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected OneTimeToken() {}
    OneTimeToken(Long userId, Type type, String tokenHash, Instant expiresAt) { this.userId = userId; this.type = type; this.tokenHash = tokenHash; this.expiresAt = expiresAt; }
    Long getUserId() { return userId; }
    Type getType() { return type; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getUsedAt() { return usedAt; }
    void markUsed() { this.usedAt = Instant.now(); }
}
