package com.yep.platform.user;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.PARTICIPANT;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private UserStatus status = UserStatus.PENDING;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "last_login_at") private Instant lastLoginAt;

    protected User() {}
    public User(String email, String passwordHash, Role role, UserStatus status) {
        this.email = email; this.passwordHash = passwordHash; this.role = role; this.status = status;
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) { this.passwordHash = h; }
    public Role getRole() { return role; }
    public void setRole(Role r) { this.role = r; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus s) { this.status = s; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int n) { this.failedAttempts = n; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant t) { this.lockedUntil = t; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant t) { this.lastLoginAt = t; }
}
