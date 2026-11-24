package com.yep.platform.security;

import com.yep.platform.user.Role;
import com.yep.platform.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User u) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(u.getId())).claim("email", u.getEmail()).claim("role", u.getRole().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(props.accessTtl()))).signWith(key).compact();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            var c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class), Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
