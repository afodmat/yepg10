package com.yep.platform.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("yep.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 bytes long");
    }
}
