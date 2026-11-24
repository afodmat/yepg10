package com.yep.platform.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {
    Optional<OneTimeToken> findByTokenHash(String hash);
}
