package com.yep.platform.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String hash);

    @Modifying @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :uid and t.revokedAt is null")
    void revokeAllForUser(@Param("uid") Long userId, @Param("now") java.time.Instant now);

    default void revokeAllForUser(Long userId) { revokeAllForUser(userId, java.time.Instant.now()); }
}
